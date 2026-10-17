package com.afs.module.rag.search;

import com.afs.module.rag.rerank.RerankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 编排 RAG 召回与候选重排，并提供检索结果的应用级查询接口。
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class RagSearchService {

    private final VectorStore vectorStore;
    private final SearchStrategyResolver strategyResolver;
    private final RerankService rerankService;
    private final FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();

    @Value("${spring.ai.rerank.enabled:false}")
    private boolean rerankEnabled;

    @Value("${spring.ai.rerank.candidate-top-k:20}")
    private int rerankCandidateTopK;

    @Cacheable(value = "vectorSearch", key = "#query + '_' + #topK", unless = "#result == null || #result.isEmpty()")
    public List<Document> searchRelevant(String query, int topK) {
        validateRequest(query, topK);
        long startTime = System.currentTimeMillis();
        SearchStrategy strategy = strategyResolver.getStrategy();
        try {
            int retrievalTopK = rerankEnabled ? Math.max(topK, rerankCandidateTopK) : topK;
            List<Document> results = strategy.search(query, null, retrievalTopK);
            if (rerankEnabled) {
                results = rerankService.rerank(query, results).stream().limit(topK).toList();
            }
            String strategyDesc = rerankEnabled ? strategy.getName() + "+rerank" : strategy.getName();
            log.info("RAG 检索完成，结果数: {}, 策略: {}, 耗时: {}ms",
                    results.size(), strategyDesc, System.currentTimeMillis() - startTime);
            return results;
        } catch (Exception e) {
            log.error("RAG 检索失败，topK: {}, 策略: {}", topK, strategy.getName(), e);
            if (!"vector".equals(strategy.getName())) {
                try {
                    SearchStrategy fallback = strategyResolver.getStrategy("vector");
                    log.warn("策略 {} 检索失败，尝试纯向量策略降级", strategy.getName());
                    List<Document> fallbackResults = fallback.search(query, null, topK);
                    if (fallbackResults == null) {
                        throw new IllegalStateException("纯向量降级策略返回了空引用");
                    }
                    return fallbackResults;
                } catch (Exception fallbackException) {
                    e.addSuppressed(fallbackException);
                    log.error("纯向量检索降级也失败", fallbackException);
                }
            }
            throw new IllegalStateException("RAG 检索失败", e);
        }
    }

    public List<Map<String, Object>> searchWithMetadata(String query, int topK) {
        return searchRelevant(query, topK).stream()
                .map(document -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("content", document.getText());
                    result.put("source", String.valueOf(document.getMetadata().getOrDefault("source", "未知来源")));
                    result.put("title", String.valueOf(document.getMetadata().getOrDefault("title", "")));
                    result.put("id", String.valueOf(document.getMetadata().getOrDefault("dbId", "")));
                    result.put("type", String.valueOf(document.getMetadata().getOrDefault("type", "knowledge")));
                    result.put("score", document.getMetadata().getOrDefault("distance", 0.0));
                    return result;
                })
                .collect(Collectors.toList());
    }

    public List<Document> searchByCategory(String query, String category, int topK) {
        validateRequest(query, topK);
        var filter = filterBuilder.and(
                filterBuilder.eq("type", "knowledge"),
                filterBuilder.eq("category", category)
        ).build();
        return vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(topK)
                .filterExpression(filter)
                .build());
    }

    public Map<String, Object> getSearchStrategyInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("reRankEnabled", rerankEnabled);
        info.put("availableStrategies", strategyResolver.getAvailableStrategies());
        info.put("currentStrategy", strategyResolver.getStrategy().getName());
        return info;
    }

    private void validateRequest(String query, int topK) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("检索查询不能为空");
        }
        if (topK <= 0) {
            throw new IllegalArgumentException("topK 必须大于 0");
        }
    }
}
