package com.afs.module.rag.search;

import com.afs.module.rag.query.DictionaryQueryExpander;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 组合 PostgreSQL 全文检索与向量检索，并使用 RRF 融合两路结果。
 * @author guanwx
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class PostgresHybridSearchService {

    /** RRF 融合常数：数值越大，排名靠后的结果分数衰减越平缓 */
    private static final int RRF_K = 60;

    /** 向量库，向量召回通道 */
    private final VectorStore vectorStore;

    /** JDBC 客户端，执行 PostgreSQL 全文检索与 chunk 回查 */
    private final JdbcTemplate jdbcTemplate;

    /** 查询扩展器，为关键词召回生成扩展查询 */
    private final DictionaryQueryExpander queryExpander;

    /** 是否启用混合检索 */
    @Value("${spring.ai.vectorstore.hybrid-search.enabled:false}")
    private boolean hybridSearchEnabled;

    /** 是否启用查询扩展 */
    @Value("${spring.ai.vectorstore.hybrid-search.query-expansion:true}")
    private boolean queryExpansionEnabled;

    /** 向量通道召回的候选数量 */
    @Value("${spring.ai.vectorstore.hybrid-search.vector-top-k:20}")
    private int vectorTopK;

    /** 关键词通道召回的候选数量 */
    @Value("${spring.ai.vectorstore.hybrid-search.keyword-top-k:20}")
    private int keywordTopK;

    /** 扩展查询的最大数量 */
    @Value("${spring.ai.vectorstore.hybrid-search.max-expansion-queries:5}")
    private int maxExpansionQueries;

    /**
     * 按当前检索配置执行向量检索或混合检索，并返回调用方需要的候选数量。
     */
    public List<Document> search(String query, Map<String, Object> filters, int topK) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("检索查询不能为空");
        }
        if (topK <= 0) {
            throw new IllegalArgumentException("topK 必须大于 0");
        }
        RagSearchFilters.validate(filters);
        if (!hybridSearchEnabled) {
            return pureVectorSearch(query, filters, topK);
        }
        if (vectorTopK <= 0 || keywordTopK <= 0 || maxExpansionQueries <= 0) {
            throw new IllegalArgumentException("混合检索各路 topK 与查询扩展上限必须大于 0");
        }

        // 查询扩展只用于关键词召回；向量召回仍使用原始问题，避免扩展词稀释语义。
        List<String> expandedQueries = queryExpansionEnabled
                ? queryExpander.expand(query).stream().filter(value -> !value.isBlank())
                        .distinct().limit(maxExpansionQueries).toList()
                : List.of(query);

        RecallResult vectorResult = vectorSearch(query, filters, vectorTopK);
        RecallResult keywordResult = fullTextSearch(expandedQueries, filters, keywordTopK);
        if ((vectorResult.failed() || keywordResult.failed())
                && vectorResult.scores().isEmpty() && keywordResult.scores().isEmpty()) {
            throw new IllegalStateException("检索通道发生错误且没有可用召回结果");
        }
        List<Document> fusedResults = rrfFusion(vectorResult.scores(), keywordResult.scores(), topK);

        log.info("混合检索完成 - 原始查询: {}, 扩展查询数: {}, 向量命中: {}, 全文检索命中: {}, 最终返回: {}",
                query, expandedQueries.size(), vectorResult.scores().size(),
                keywordResult.scores().size(), fusedResults.size());
        return fusedResults;
    }

    /**
     * 混合检索关闭时的直连通道：仅做纯向量相似度检索。
     */
    private List<Document> pureVectorSearch(String query, Map<String, Object> filters, int topK) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(topK);
        var filter = RagSearchFilters.buildVectorExpression(filters);
        if (filter != null) {
            builder.filterExpression(filter);
        }
        return vectorStore.similaritySearch(builder.build());
    }

    /**
     * 向量通道召回：把名次转换为 RRF 分数（1 / (K + rank)）；失败时返回空分数表并标记 failed，由调用方决定兜底。
     */
    private RecallResult vectorSearch(String query, Map<String, Object> filters, int topK) {
        Map<String, Double> scores = new LinkedHashMap<>();
        try {
            SearchRequest.Builder request = SearchRequest.builder().query(query).topK(topK);
            var filter = RagSearchFilters.buildVectorExpression(filters);
            if (filter != null) {
                request.filterExpression(filter);
            }
            List<Document> results = vectorStore.similaritySearch(request.build());
            // RRF 只比较排名，不直接混用向量距离和全文相关性分数。
            for (int rank = 0; rank < results.size(); rank++) {
                scores.put(results.get(rank).getId(), 1.0 / (RRF_K + rank + 1));
            }
            return new RecallResult(scores, false);
        } catch (Exception e) {
            log.error("向量检索失败，将仅使用全文检索结果参与融合", e);
            return new RecallResult(scores, true);
        }
    }

    /**
     * 关键词通道召回：对每个（含扩展）查询做全文检索，同一 chunk 取最佳名次后转换为 RRF 分数。
     */
    private RecallResult fullTextSearch(List<String> queries, Map<String, Object> filters, int topK) {
        Map<String, Integer> bestRanks = new HashMap<>();
        boolean failed = false;
        for (String query : queries) {
            // 查询词与写入索引时使用相同的分词器，避免中英文分词规则不一致。
            List<String> terms = FullTextTokenizer.tokenize(query).stream().distinct().toList();
            if (terms.isEmpty()) {
                continue;
            }

            try {
                List<Map<String, Object>> results = runFullTextQuery(terms, filters, topK);
                for (int rank = 0; rank < results.size(); rank++) {
                    String chunkId = results.get(rank).get("chunk_id").toString();
                    // 扩展查询属于同一关键词通道，同一 chunk 只保留其最佳名次，避免重复加权。
                    bestRanks.merge(chunkId, rank + 1, Math::min);
                }
            } catch (Exception e) {
                failed = true;
                log.error("PostgreSQL 全文检索失败，查询: {}", query, e);
            }
        }
        Map<String, Double> scores = new LinkedHashMap<>();
        bestRanks.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().thenComparing(Map.Entry::getKey))
                .forEach(entry -> scores.put(entry.getKey(), 1.0 / (RRF_K + entry.getValue())));
        return new RecallResult(scores, failed);
    }

    /**
     * 执行一次 PostgreSQL 全文查询：词项以 OR 组合成 tsquery，按 ts_rank_cd 相关性排序取 topK。
     */
    private List<Map<String, Object>> runFullTextQuery(List<String> terms,
                                                       Map<String, Object> filters, int topK) {
        // 词项经转义后作为参数绑定；字段和值过滤也都使用参数，避免拼接用户输入。
        StringBuilder sql = new StringBuilder("""
                SELECT c.chunk_id,
                       ts_rank_cd(c.search_vector, to_tsquery('simple', ?)) AS score
                FROM rag_chunk c
                WHERE c.search_vector @@ to_tsquery('simple', ?)
                """);
        List<Object> parameters = new ArrayList<>();
        String tsQuery = terms.stream()
                .map(term -> "'" + term.replace("'", "''") + "'")
                .collect(Collectors.joining(" | "));
        parameters.add(tsQuery);
        parameters.add(tsQuery);
        appendFilterConditions(sql, parameters, filters, "c", " AND ");
        sql.append(" ORDER BY score DESC, c.chunk_id LIMIT ?");
        parameters.add(topK);
        return jdbcTemplate.queryForList(sql.toString(), parameters.toArray());
    }

    /**
     * 向全文 SQL 追加过滤条件；caseType 是复合条件（来源类型 + 案例分类），其余字段走白名单映射。
     */
    private void appendFilterConditions(StringBuilder sql, List<Object> parameters,
                                        Map<String, Object> filters, String alias, String firstPrefix) {
        if (filters == null || filters.isEmpty()) {
            return;
        }

        Map<String, String> columns = Map.of(
                "type", "source_type",
                "dbId", "source_id",
                "category", "category"
        );
        // 过滤字段名来自固定白名单，过滤值仍通过 JDBC 参数绑定。
        boolean firstCondition = true;
        for (Map.Entry<String, Object> filter : filters.entrySet()) {
            if (filter.getValue() == null) {
                continue;
            }
            if ("caseType".equals(filter.getKey())) {
                sql.append(firstCondition ? firstPrefix : " AND ")
                        .append(alias).append(".source_type = ? AND ")
                        .append(alias).append(".category = ?");
                parameters.add("scamcase");
                parameters.add(filter.getValue());
                firstCondition = false;
                continue;
            }
            String column = columns.get(filter.getKey());
            if (column == null) {
                throw new IllegalArgumentException("不支持的混合检索过滤字段: " + filter.getKey());
            }
            sql.append(firstCondition ? firstPrefix : " AND ")
                    .append(alias).append('.').append(column).append(" = ?");
            parameters.add("dbId".equals(filter.getKey())
                    ? Long.valueOf(filter.getValue().toString())
                    : filter.getValue());
            firstCondition = false;
        }
    }

    /**
     * RRF 融合两路召回：按 chunk_id 累加两路的倒数排名分数，排序后取 topK 并回填文档内容。
     */
    private List<Document> rrfFusion(Map<String, Double> vectorScores,
                                     Map<String, Double> keywordScores,
                                     int topK) {
        // 两路召回共用 chunk ID，因此可直接按 ID 累加倒数排名分数。
        Set<String> chunkIds = new LinkedHashSet<>();
        chunkIds.addAll(vectorScores.keySet());
        chunkIds.addAll(keywordScores.keySet());

        Map<String, Double> fusedScores = new HashMap<>();
        for (String chunkId : chunkIds) {
            fusedScores.put(chunkId,
                    vectorScores.getOrDefault(chunkId, 0.0) + keywordScores.getOrDefault(chunkId, 0.0));
        }

        List<String> rankedIds = fusedScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed()
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .toList();
        return getDocumentsByIds(rankedIds, fusedScores).stream()
                .limit(topK)
                .toList();
    }

    /**
     * 按 chunk_id 批量回查 rag_chunk，重建带元数据的 Document（distance 字段写入融合分数）。
     */
    private List<Document> getDocumentsByIds(List<String> chunkIds, Map<String, Double> scores) {
        if (chunkIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 用单次批量查询恢复融合后的原始文本和元数据，避免逐条回查。
        String placeholders = String.join(",", Collections.nCopies(chunkIds.size(), "?"));
        String sql = "SELECT chunk_id, source_type, source_id, title, category, content "
                + "FROM rag_chunk WHERE chunk_id IN (" + placeholders + ")";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, chunkIds.toArray());
        Map<String, Document> documentsById = new HashMap<>();

        for (Map<String, Object> row : rows) {
            String chunkId = row.get("chunk_id").toString();
            String type = row.get("source_type").toString();
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("dbId", row.get("source_id").toString());
            metadata.put("title", row.get("title"));
            metadata.put("type", type);
            metadata.put("source", "scamcase".equals(type) ? "诈骗案例" : "知识库");
            metadata.put("distance", scores.getOrDefault(chunkId, 0.0));
            if (row.get("category") != null) {
                metadata.put("category", row.get("category"));
                if ("scamcase".equals(type)) {
                    metadata.put("caseType", row.get("category"));
                }
            }

            documentsById.put(chunkId, Document.builder()
                    .id(chunkId)
                    .text(row.get("content").toString())
                    .metadata(metadata)
                    .build());
        }

        return chunkIds.stream()
                .map(documentsById::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 输出混合检索配置与查询扩展明细的调试文本。
     */
    public String getSearchStats(String query) {
        StringBuilder stats = new StringBuilder();
        stats.append("=== 混合检索配置 ===\n");
        stats.append(String.format("混合检索: %s\n", hybridSearchEnabled ? "启用" : "禁用"));
        stats.append(String.format("查询扩展: %s\n", queryExpansionEnabled ? "启用" : "禁用"));
        stats.append(String.format("向量检索TopK: %d\n", vectorTopK));
        stats.append(String.format("全文检索TopK: %d\n", keywordTopK));
        stats.append(String.format("最终返回数量由请求 topK 决定\n"));
        stats.append(String.format("RRF融合参数K: %d\n", RRF_K));
        if (queryExpansionEnabled) {
            stats.append("\n=== 查询扩展 ===\n");
            stats.append(queryExpander.getExpansionInfo(query));
        }
        return stats.toString();
    }

    /** 单路召回结果：chunk_id → RRF 分数，以及该路是否发生过错误 */
    private record RecallResult(Map<String, Double> scores, boolean failed) {
    }
}
