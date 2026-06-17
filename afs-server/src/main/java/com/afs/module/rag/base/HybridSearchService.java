package com.afs.module.rag.base;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合检索服务
 * 
 * 结合 BM25 关键词检索和向量检索的优势，通过 RRF 算法融合结果
 * 
 * 解决的问题：
 * 1. 用户用词与文档用词不一致时，纯向量检索效果差
 * 2. 纯关键词检索无法理解语义
 * 
 * 实现原理：
 * 1. BM25 检索：根据关键词匹配度计算相关性分数
 * 2. 向量检索：根据语义相似度计算相关性分数
 * 3. RRF 融合：将两种检索结果按排名融合
 * 
 * RRF (Reciprocal Rank Fusion) 算法：
 * score(d) = Σ 1/(k + rank_i(d))
 * 
 * 其中：
 * - d: 文档
 * - rank_i(d): 文档在第 i 种检索结果中的排名（从1开始）
 * - k: 融合参数，通常取 60，k 越大，各检索结果的权重越均衡
 * 
 * @author AFS
 */
@Slf4j
@Service
public class HybridSearchService {

    /**
     * RRF 融合算法的参数
     * k 值越大，各检索结果的权重越均衡
     * 通常取值范围：60 ~ 100
     */
    private static final int RRF_K = 60;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private QueryExpander queryExpander;

    /**
     * 是否启用混合检索
     */
    @Value("${spring.ai.vectorstore.hybrid-search.enabled:true}")
    private boolean hybridSearchEnabled;

    /**
     * 是否启用查询扩展
     */
    @Value("${spring.ai.vectorstore.hybrid-search.query-expansion:true}")
    private boolean queryExpansionEnabled;

    /**
     * 向量检索返回的文档数量（用于 RRF 融合）
     */
    @Value("${spring.ai.vectorstore.hybrid-search.vector-top-k:20}")
    private int vectorTopK;

    /**
     * BM25 检索返回的文档数量（用于 RRF 融合）
     */
    @Value("${spring.ai.vectorstore.hybrid-search.bm25-top-k:20}")
    private int bm25TopK;

    /**
     * 最终返回的文档数量
     */
    @Value("${spring.ai.vectorstore.hybrid-search.final-top-k:5}")
    private int finalTopK;

    /**
     * 混合检索主方法
     * 
     * @param query 用户查询
     * @param filters 过滤条件（可选）
     * @return 融合后的文档列表
     */
    public List<Document> search(String query, Map<String, Object> filters) {
        if (!hybridSearchEnabled) {
            // 如果未启用混合检索，降级为纯向量检索
            return pureVectorSearch(query, filters);
        }

        List<String> expandedQueries = Collections.singletonList(query);
        if (queryExpansionEnabled) {
            // 如果启用了查询扩展，扩展查询
            expandedQueries = queryExpander.expand(query).stream()
                    .filter(q -> !q.isEmpty())
                    .collect(Collectors.toList());
        }

        // 1. 向量检索
        Map<String, Double> vectorScores = vectorSearch(query, filters, vectorTopK);

        // 2. BM25 关键词检索（使用扩展后的所有查询）
        Map<String, Double> bm25Scores = bm25Search(expandedQueries, filters, bm25TopK);

        // 3. RRF 融合
        List<Document> fusedResults = rrfFusion(vectorScores, bm25Scores, filters);

        log.info("混合检索完成 - 原始查询: {}, 扩展查询数: {}, 向量命中: {}, BM25命中: {}, 最终返回: {}",
                query, expandedQueries.size(), vectorScores.size(), bm25Scores.size(), fusedResults.size());

        return fusedResults;
    }

    /**
     * 纯向量检索（降级方案）
     */
    private List<Document> pureVectorSearch(String query, Map<String, Object> filters) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(finalTopK);

        if (filters != null && !filters.isEmpty()) {
            builder.filterExpression(buildFilterExpression(filters));
        }

        return vectorStore.similaritySearch(builder.build());
    }

    /**
     * 向量检索
     * 
     * @param query 用户查询
     * @param filters 过滤条件
     * @param topK 返回数量
     * @return 文档ID -> RRF分数 的映射
     */
    private Map<String, Double> vectorSearch(String query, Map<String, Object> filters, int topK) {
        Map<String, Double> scores = new LinkedHashMap<>();

        try {
            SearchRequest.Builder builder = SearchRequest.builder()
                    .query(query)
                    .topK(topK);

            if (filters != null && !filters.isEmpty()) {
                builder.filterExpression(buildFilterExpression(filters));
            }

            List<Document> results = vectorStore.similaritySearch(builder.build());

            // 按排名计算 RRF 分数
            for (int rank = 0; rank < results.size(); rank++) {
                String docId = results.get(rank).getId();
                double rrfScore = 1.0 / (RRF_K + rank + 1);
                scores.put(docId, rrfScore);
            }
        } catch (Exception e) {
            log.error("向量检索失败: {}", e.getMessage());
        }

        return scores;
    }

    /**
     * BM25 关键词检索
     * 
     * 使用 PostgreSQL 全文检索实现 BM25 效果
     * 
     * @param queries 扩展后的查询列表
     * @param filters 过滤条件
     * @param topK 返回数量
     * @return 文档ID -> RRF分数 的映射
     */
    private Map<String, Double> bm25Search(List<String> queries, Map<String, Object> filters, int topK) {
        Map<String, Double> scores = new LinkedHashMap<>();

        try {
            // 对每个扩展查询执行 BM25 检索
            for (String query : queries) {
                String sql = buildBm25Sql(query, filters, topK);
                List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

                // 按排名计算 RRF 分数并累加
                for (int rank = 0; rank < results.size(); rank++) {
                    String docId = results.get(rank).get("id").toString();
                    double rrfScore = 1.0 / (RRF_K + rank + 1);
                    
                    // 累加同一文档在不同查询中的分数
                    scores.merge(docId, rrfScore, Double::sum);
                }
            }
        } catch (Exception e) {
            log.error("BM25检索失败: {}", e.getMessage());
        }

        return scores;
    }

    /**
     * 构建 BM25 SQL 查询
     */
    private String buildBm25Sql(String query, Map<String, Object> filters, int topK) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id, ts_rank(search_vector, plainto_tsquery('simple', ?)) as rank ");
        sql.append("FROM knowledge ");
        sql.append("WHERE search_vector @@ plainto_tsquery('simple', ?) ");
        
        // 添加过滤条件
        if (filters != null && filters.containsKey("status")) {
            sql.append(" AND status = '").append(filters.get("status")).append("' ");
        }
        if (filters != null && filters.containsKey("type")) {
            sql.append(" AND '").append(filters.get("type")).append("' = ANY(string_to_array(coalesce(metadata->>'type', ''), ',')) ");
        }
        
        sql.append("ORDER BY rank DESC ");
        sql.append("LIMIT ").append(topK);
        
        return sql.toString();
    }

    /**
     * RRF 融合算法
     * 
     * 将向量检索和 BM25 检索的结果按排名融合
     * 
     * @param vectorScores 向量检索分数
     * @param bm25Scores BM25 检索分数
     * @param filters 过滤条件
     * @return 融合后的文档列表
     */
    private List<Document> rrfFusion(Map<String, Double> vectorScores, 
                                       Map<String, Double> bm25Scores,
                                       Map<String, Object> filters) {
        // 合并所有文档ID
        Set<String> allDocIds = new LinkedHashSet<>();
        allDocIds.addAll(vectorScores.keySet());
        allDocIds.addAll(bm25Scores.keySet());

        // 计算融合分数
        Map<String, Double> fusedScores = new LinkedHashMap<>();
        for (String docId : allDocIds) {
            double vectorScore = vectorScores.getOrDefault(docId, 0.0);
            double bm25Score = bm25Scores.getOrDefault(docId, 0.0);
            fusedScores.put(docId, vectorScore + bm25Score);
        }

        // 按融合分数排序
        List<String> sortedDocIds = fusedScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(finalTopK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 获取文档详情
        return getDocumentsByIds(sortedDocIds);
    }

    /**
     * 根据文档ID列表获取文档详情
     */
    private List<Document> getDocumentsByIds(List<String> docIds) {
        if (docIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Document> documents = new ArrayList<>();
        
        for (String docId : docIds) {
            try {
                // 从向量存储中搜索特定ID的文档
                List<Document> docs = vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query("__id__:" + docId)
                                .topK(1)
                                .build()
                );
                
                // 如果没找到，尝试从数据库直接获取
                if (docs.isEmpty()) {
                    docs = getDocumentFromDatabase(docId);
                }
                
                if (!docs.isEmpty()) {
                    documents.add(docs.get(0));
                }
            } catch (Exception e) {
                log.warn("获取文档 {} 失败: {}", docId, e.getMessage());
            }
        }
        
        return documents;
    }

    /**
     * 从数据库直接获取文档（备用方案）
     */
    private List<Document> getDocumentFromDatabase(String docId) {
        try {
            String sql = "SELECT id, title, content, metadata FROM knowledge WHERE id = ?";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, Long.parseLong(docId));
            
            return results.stream()
                    .map(row -> {
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("dbId", row.get("id"));
                        metadata.put("title", row.get("title"));
                        metadata.put("type", "knowledge");
                        
                        Object metaObj = row.get("metadata");
                        if (metaObj instanceof Map) {
                            Map<?, ?> existingMeta = (Map<?, ?>) metaObj;
                            existingMeta.forEach((k, v) -> metadata.put(k.toString(), v));
                        }
                        
                        return new Document(
                                row.get("id").toString(),
                                row.get("content") != null ? row.get("content").toString() : "",
                                metadata
                        );
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("从数据库获取文档 {} 失败: {}", docId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 构建过滤表达式
     */
    private org.springframework.ai.vectorstore.filter.Filter.Expression buildFilterExpression(Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return null;
        }

        var filterBuilder = new org.springframework.ai.vectorstore.filter.FilterExpressionBuilder();
        
        // 如果只有一个条件
        if (filters.size() == 1) {
            Map.Entry<String, Object> entry = filters.entrySet().iterator().next();
            return filterBuilder.eq(entry.getKey(), entry.getValue().toString()).build();
        }
        
        // 多个条件：收集所有表达式然后用 AND 连接
        List<org.springframework.ai.vectorstore.filter.FilterExpressionBuilder.Op> ops = new ArrayList<>();
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            ops.add(filterBuilder.eq(entry.getKey(), entry.getValue().toString()));
        }
        
        // 使用 reduce 方式连接所有条件
        return ops.stream()
                .reduce((op1, op2) -> filterBuilder.and(op1, op2))
                .map(op -> op.build())
                .orElse(null);
    }

    /**
     * 获取检索统计信息（用于调试）
     */
    public String getSearchStats(String query) {
        StringBuilder stats = new StringBuilder();
        stats.append("=== 混合检索配置 ===\n");
        stats.append(String.format("混合检索: %s\n", hybridSearchEnabled ? "启用" : "禁用"));
        stats.append(String.format("查询扩展: %s\n", queryExpansionEnabled ? "启用" : "禁用"));
        stats.append(String.format("向量检索TopK: %d\n", vectorTopK));
        stats.append(String.format("BM25检索TopK: %d\n", bm25TopK));
        stats.append(String.format("最终返回数量: %d\n", finalTopK));
        stats.append(String.format("RRF融合参数K: %d\n", RRF_K));
        
        if (queryExpansionEnabled) {
            stats.append("\n=== 查询扩展 ===\n");
            stats.append(queryExpander.getExpansionInfo(query));
        }
        
        return stats.toString();
    }
}
