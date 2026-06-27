package com.afs.module.rag.base;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ReRank 重排序服务
 * 
 * 使用交叉编码器对候选文档进行精细化打分，提升检索准确率
 * 
 * 解决的问题：
 * 1. 向量检索和 BM25 检索的初步结果可能不够精确
 * 2. 需要更精细的 query-document 相关性评估
 * 
 * 实现原理：
 * 1. 交叉编码器：同时将 query 和 document 输入模型
 * 2. 输出一个相关性分数（0-1 之间）
 * 3. 按分数重新排序
 * 
 * 使用场景：
 * - 混合检索后的结果需要进一步优化
 * - 对回答质量要求较高的场景
 * - 需要精准匹配的场景
 * 
 * 推荐的免费 ReRank API：
 * 1. Jina Reranker: https://jina.ai/reranker/ (免费 1 万 token)
 * 2. Cohere Rerank: https://cohere.com/rerank (免费 API)
 * 
 */
@Slf4j
@Service
public class ReRankService {

    /**
     * 是否启用 ReRank
     */
    @Value("${spring.ai.rerank.enabled:false}")
    private boolean rerankEnabled;

    /**
     * ReRank API 类型: jina / cohere / mock
     */
    @Value("${spring.ai.rerank.provider:jina}")
    private String rerankProvider;

    /**
     * Jina Rerank API Endpoint
     */
    @Value("${spring.ai.rerank.jina.endpoint:https://api.jina.ai/v1/rerank}")
    private String jinaEndpoint;

    /**
     * Jina API Key
     */
    @Value("${spring.ai.rerank.jina.api-key:}")
    private String jinaApiKey;

    /**
     * Cohere API Endpoint
     */
    @Value("${spring.ai.rerank.cohere.endpoint:https://api.cohere.ai/v1/rerank}")
    private String cohereEndpoint;

    /**
     * Cohere API Key
     */
    @Value("${spring.ai.rerank.cohere.api-key:}")
    private String cohereApiKey;

    /**
     * ReRank 返回的 Top N 结果
     */
    @Value("${spring.ai.rerank.top-n:3}")
    private int rerankTopN;

    /**
     * RestTemplate 用于调用外部 API
     */
    private RestTemplate restTemplate;

    @Autowired(required = false)
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * ReRank 重排序主方法
     * 
     * @param query 用户查询
     * @param candidates 候选文档列表（通常是混合检索的结果）
     * @return 重排序后的文档列表
     */
    public List<Document> rerank(String query, List<Document> candidates) {
        if (!rerankEnabled) {
            // 如果未启用 ReRank，直接返回原始结果
            return candidates;
        }

        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            switch (rerankProvider.toLowerCase()) {
                case "jina":
                    return rerankWithJina(query, candidates);
                case "cohere":
                    return rerankWithCohere(query, candidates);
                case "mock":
                default:
                    return rerankWithMock(query, candidates);
            }
        } catch (Exception e) {
            log.error("ReRank 调用失败: {}, 降级为原始结果", e.getMessage());
            return candidates;
        }
    }

    /**
     * 使用 Jina Rerank API 进行重排序
     * 
     * API 文档: https://jina.ai/reranker/
     * 
     * 请求格式:
     * {
     *   "model": "jina-reranker-v2-base-multilingual",
     *   "query": "用户查询",
     *   "documents": ["文档1内容", "文档2内容", ...],
     *   "top_n": 3
     * }
     * 
     * 响应格式:
     * {
     *   "results": [
     *     {"index": 0, "relevance_score": 0.95},
     *     {"index": 1, "relevance_score": 0.85},
     *     ...
     *   ]
     * }
     */
    private List<Document> rerankWithJina(String query, List<Document> candidates) {
        if (jinaApiKey == null || jinaApiKey.isEmpty()) {
            log.warn("Jina API Key 未配置，使用 Mock ReRank");
            return rerankWithMock(query, candidates);
        }

        // 准备请求
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "jina-reranker-v2-base-multilingual");
        requestBody.put("query", query);
        List<Object> documentList = candidates.stream()
                .map(doc -> (Object) doc.getText())
                .collect(Collectors.toList());
        requestBody.put("documents", documentList);
        requestBody.put("top_n", rerankTopN);

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jinaApiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        // 发送请求
        ResponseEntity<Map> response = restTemplate.exchange(
                jinaEndpoint,
                HttpMethod.POST,
                request,
                Map.class
        );

        // 解析响应
        List<Document> rerankedDocs = new ArrayList<>();
        if (response.getBody() != null && response.getBody().containsKey("results")) {
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("results");
            
            for (Map<String, Object> result : results) {
                int index = ((Number) result.get("index")).intValue();
                rerankedDocs.add(candidates.get(index));
            }
        }

        log.info("Jina ReRank 完成 - 输入: {}, 输出: {}", candidates.size(), rerankedDocs.size());
        return rerankedDocs;
    }

    /**
     * 使用 Cohere Rerank API 进行重排序
     * 
     * API 文档: https://docs.cohere.com/docs/reranking
     */
    private List<Document> rerankWithCohere(String query, List<Document> candidates) {
        if (cohereApiKey == null || cohereApiKey.isEmpty()) {
            log.warn("Cohere API Key 未配置，使用 Mock ReRank");
            return rerankWithMock(query, candidates);
        }

        // 准备请求
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "rerank-multilingual-v3.0");
        requestBody.put("query", query);
        List<Object> documentList = candidates.stream()
                .map(doc -> (Object) doc.getText())
                .collect(Collectors.toList());
        requestBody.put("documents", documentList);
        requestBody.put("top_n", rerankTopN);
        requestBody.put("return_documents", false);

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + cohereApiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        // 发送请求
        ResponseEntity<Map> response = restTemplate.exchange(
                cohereEndpoint,
                HttpMethod.POST,
                request,
                Map.class
        );

        // 解析响应
        List<Document> rerankedDocs = new ArrayList<>();
        if (response.getBody() != null && response.getBody().containsKey("results")) {
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("results");
            
            for (Map<String, Object> result : results) {
                int index = ((Number) result.get("index")).intValue();
                rerankedDocs.add(candidates.get(index));
            }
        }

        log.info("Cohere ReRank 完成 - 输入: {}, 输出: {}", candidates.size(), rerankedDocs.size());
        return rerankedDocs;
    }

    /**
     * Mock ReRank 实现（用于测试或 API 不可用时）
     * 
     * 实现思路：
     * 1. 计算 query 和 document 的关键词重合度
     * 2. 结合语义相似度（如果有embedding）
     * 3. 返回排序后的结果
     */
    private List<Document> rerankWithMock(String query, List<Document> candidates) {
        String[] queryWords = query.toLowerCase().split("\\s+");
        
        // 计算每个文档的得分
        List<Map.Entry<Document, Double>> scoredDocs = new ArrayList<>();
        
        for (Document doc : candidates) {
            String content = (doc.getText() != null ? doc.getText() : "").toLowerCase();
            String title = doc.getMetadata().getOrDefault("title", "").toString().toLowerCase();
            
            double score = 0.0;
            
            // 1. 标题关键词匹配（权重更高）
            for (String word : queryWords) {
                if (title.contains(word)) {
                    score += 2.0; // 标题匹配权重
                }
            }
            
            // 2. 内容关键词匹配
            for (String word : queryWords) {
                if (content.contains(word)) {
                    score += 1.0; // 内容匹配权重
                }
                // 统计词频
                int count = 0;
                int index = 0;
                while ((index = content.indexOf(word, index)) != -1) {
                    count++;
                    index += word.length();
                }
                score += count * 0.1; // 词频加分
            }
            
            // 3. 开头匹配加分（文档开头的内容通常更重要）
            String first100 = content.length() > 100 ? content.substring(0, 100) : content;
            for (String word : queryWords) {
                if (first100.contains(word)) {
                    score += 0.5;
                }
            }
            
            scoredDocs.add(new AbstractMap.SimpleEntry<>(doc, score));
        }
        
        // 按得分排序
        scoredDocs.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        
        // 取 Top N
        List<Document> result = scoredDocs.stream()
                .limit(rerankTopN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        log.info("Mock ReRank 完成 - 输入: {}, 输出: {}", candidates.size(), result.size());
        return result;
    }

    /**
     * 计算文档的相关性分数（用于调试和分析）
     */
    public Map<String, Double> calculateRelevanceScores(String query, List<Document> candidates) {
        Map<String, Double> scores = new LinkedHashMap<>();
        
        for (Document doc : candidates) {
            String docId = doc.getId();
            String content = (doc.getText() != null ? doc.getText() : "").toLowerCase();
            String title = doc.getMetadata().getOrDefault("title", "").toString().toLowerCase();
            
            String[] queryWords = query.toLowerCase().split("\\s+");
            double score = 0.0;
            
            // 标题匹配
            for (String word : queryWords) {
                if (title.contains(word)) score += 2.0;
            }
            
            // 内容匹配
            for (String word : queryWords) {
                if (content.contains(word)) score += 1.0;
            }
            
            scores.put(docId, score);
        }
        
        return scores;
    }

    /**
     * 获取 ReRank 配置信息（用于调试）
     */
    public String getConfigInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== ReRank 配置 ===\n");
        info.append(String.format("启用状态: %s\n", rerankEnabled ? "启用" : "禁用"));
        info.append(String.format("Provider: %s\n", rerankProvider));
        info.append(String.format("TopN: %d\n", rerankTopN));
        
        if ("jina".equalsIgnoreCase(rerankProvider)) {
            info.append(String.format("Jina Endpoint: %s\n", jinaEndpoint));
            info.append(String.format("Jina API Key: %s\n", 
                    jinaApiKey != null && !jinaApiKey.isEmpty() ? "已配置" : "未配置"));
        } else if ("cohere".equalsIgnoreCase(rerankProvider)) {
            info.append(String.format("Cohere Endpoint: %s\n", cohereEndpoint));
            info.append(String.format("Cohere API Key: %s\n", 
                    cohereApiKey != null && !cohereApiKey.isEmpty() ? "已配置" : "未配置"));
        }
        
        return info.toString();
    }
}
