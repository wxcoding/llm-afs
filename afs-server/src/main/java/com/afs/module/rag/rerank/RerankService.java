package com.afs.module.rag.rerank;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
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
 * 1. 向量检索和全文关键词检索的初步结果可能不够精确
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
 * @author guanwx
 */
@Slf4j
@Service
public class RerankService {

    /**
     * 是否启用 ReRank
     */
    @Value("${spring.ai.rerank.enabled:false}")
    private boolean rerankEnabled;

    /**
     * ReRank API 类型: dashscope / jina / cohere / mock
     */
    @Value("${spring.ai.rerank.provider:jina}")
    private String rerankProvider;

    /**
     * DashScope ReRank API Endpoint（北京地域）
     */
    @Value("${spring.ai.rerank.dashscope.endpoint:https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank}")
    private String dashScopeEndpoint;

    /**
     * DashScope API Key（与 chat/embedding 共用百炼 Key）
     */
    @Value("${spring.ai.rerank.dashscope.api-key:}")
    private String dashScopeApiKey;

    /**
     * DashScope ReRank 模型名
     */
    @Value("${spring.ai.rerank.dashscope.model:qwen3.7-text-rerank}")
    private String dashScopeModel;

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
    private final RestTemplate restTemplate;

    /**
     * 创建 ReRank 服务并注入 HTTP 客户端。
     *
     * @param restTemplate 调用外部 ReRank API 使用的 RestTemplate
     */
    public RerankService(RestTemplate restTemplate) {
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
        if (rerankTopN <= 0) {
            throw new IllegalArgumentException("ReRank top-n 必须大于 0");
        }

        try {
            switch (rerankProvider.toLowerCase(Locale.ROOT)) {
                case "dashscope":
                    return rerankWithDashScope(query, candidates);
                case "jina":
                    return rerankWithJina(query, candidates);
                case "cohere":
                    return rerankWithCohere(query, candidates);
                case "mock":
                    return rerankWithMock(query, candidates);
                default:
                    throw new IllegalArgumentException("不支持的 ReRank provider: " + rerankProvider);
            }
        } catch (Exception e) {
            log.warn("ReRank 调用失败，保留原始召回顺序。provider: {}", rerankProvider, e);
            return candidates;
        }
    }

    /**
     * 使用阿里云百炼 DashScope ReRank API 进行重排序
     *
     * API 文档: https://help.aliyun.com/zh/model-studio/text-rerank-api
     *
     * 请求格式:
     * {
     *   "model": "qwen3.7-text-rerank",
     *   "input": { "query": "用户查询", "documents": ["文档1", "..."] },
     *   "parameters": { "top_n": 5 }
     * }
     *
     * 响应格式:
     * {
     *   "output": { "results": [ {"index": 0, "relevance_score": 0.93}, ... ] },
     *   "usage": { "total_tokens": 79 }
     * }
     */
    private List<Document> rerankWithDashScope(String query, List<Document> candidates) {
        if (dashScopeApiKey == null || dashScopeApiKey.isEmpty()) {
            throw new IllegalStateException("DashScope ReRank API Key 未配置");
        }
        if (restTemplate == null) {
            throw new IllegalStateException("ReRank HTTP 客户端未配置");
        }

        Map<String, Object> input = new HashMap<>();
        input.put("query", query);
        input.put("documents", candidates.stream()
                .map(Document::getText)
                .collect(Collectors.toList()));

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("top_n", Math.min(rerankTopN, candidates.size()));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", dashScopeModel);
        requestBody.put("input", input);
        requestBody.put("parameters", parameters);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(dashScopeApiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                dashScopeEndpoint,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<>() { }
        );

        Map<String, Object> body = response.getBody();
        if (body == null || !(body.get("output") instanceof Map)) {
            throw new IllegalStateException("DashScope ReRank 响应缺少 output 对象");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> output = (Map<String, Object>) body.get("output");
        List<Document> rerankedDocs = parseResults(output, candidates);
        log.info("DashScope ReRank 完成 - 模型: {}, 输入: {}, 输出: {}",
                dashScopeModel, candidates.size(), rerankedDocs.size());
        return rerankedDocs;
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
            throw new IllegalStateException("Jina API Key 未配置");
        }
        if (restTemplate == null) {
            throw new IllegalStateException("ReRank HTTP 客户端未配置");
        }

        // 准备请求
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "jina-reranker-v2-base-multilingual");
        requestBody.put("query", query);
        List<Object> documentList = candidates.stream()
                .map(doc -> (Object) doc.getText())
                .collect(Collectors.toList());
        requestBody.put("documents", documentList);
        requestBody.put("top_n", Math.min(rerankTopN, candidates.size()));

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jinaApiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        // 发送请求
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                jinaEndpoint,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<>() { }
        );

        List<Document> rerankedDocs = parseResults(response.getBody(), candidates);
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
            throw new IllegalStateException("Cohere API Key 未配置");
        }
        if (restTemplate == null) {
            throw new IllegalStateException("ReRank HTTP 客户端未配置");
        }

        // 准备请求
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "rerank-multilingual-v3.0");
        requestBody.put("query", query);
        List<Object> documentList = candidates.stream()
                .map(doc -> (Object) doc.getText())
                .collect(Collectors.toList());
        requestBody.put("documents", documentList);
        requestBody.put("top_n", Math.min(rerankTopN, candidates.size()));
        requestBody.put("return_documents", false);

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + cohereApiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        // 发送请求
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                cohereEndpoint,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<>() { }
        );

        List<Document> rerankedDocs = parseResults(response.getBody(), candidates);
        log.info("Cohere ReRank 完成 - 输入: {}, 输出: {}", candidates.size(), rerankedDocs.size());
        return rerankedDocs;
    }

    /**
     * 解析 ReRank 响应的 results 列表：按 index 映射回候选文档，校验索引越界与重复，截断到 topN。
     */
    private List<Document> parseResults(Map<String, Object> responseBody, List<Document> candidates) {
        if (responseBody == null || !(responseBody.get("results") instanceof List<?> results)) {
            throw new IllegalStateException("ReRank 响应缺少 results 列表");
        }

        List<Document> reranked = new ArrayList<>();
        Set<Integer> seenIndexes = new HashSet<>();
        for (Object item : results) {
            if (!(item instanceof Map<?, ?> result) || !(result.get("index") instanceof Number indexValue)) {
                throw new IllegalStateException("ReRank 响应包含无效的结果项");
            }
            int index = indexValue.intValue();
            if (index < 0 || index >= candidates.size() || !seenIndexes.add(index)) {
                throw new IllegalStateException("ReRank 响应包含越界或重复的候选索引");
            }
            reranked.add(candidates.get(index));
            if (reranked.size() == rerankTopN) {
                break;
            }
        }
        if (reranked.isEmpty()) {
            throw new IllegalStateException("ReRank 响应没有有效候选结果");
        }
        return reranked;
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
        
        if ("dashscope".equalsIgnoreCase(rerankProvider)) {
            info.append(String.format("DashScope Endpoint: %s\n", dashScopeEndpoint));
            info.append(String.format("DashScope Model: %s\n", dashScopeModel));
            info.append(String.format("DashScope API Key: %s\n",
                    dashScopeApiKey != null && !dashScopeApiKey.isEmpty() ? "已配置" : "未配置"));
        } else if ("jina".equalsIgnoreCase(rerankProvider)) {
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
