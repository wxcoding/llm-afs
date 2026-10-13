package com.afs.module.rag.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 纯向量检索策略
 * 
 * 基于向量相似度进行语义检索，适用于：
 * - 需要理解用户意图的场景
 * - 文档内容与查询词语义相关但用词不同的场景
 * 
 * 特点：
 * - 优点：语义理解能力强
 * - 缺点：对关键词变化敏感，召回率可能较低
 * 
 */
@RequiredArgsConstructor
@Slf4j
@Component("vectorSearchStrategy")
public class VectorSearchStrategy implements SearchStrategy {

    private final VectorStore vectorStore;

    @Override
    public List<Document> search(String query, Map<String, Object> filters, int topK) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("检索查询不能为空");
        }
        if (topK <= 0) {
            throw new IllegalArgumentException("topK 必须大于 0");
        }
        RagSearchFilters.validate(filters);
        log.debug("使用纯向量检索策略，查询词: {}, topK: {}", query, topK);
        
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(0.5);

        // 所有有效条件以 AND 组合；案例类型同时限定来源类型与案例分类。
        var filter = RagSearchFilters.buildVectorExpression(filters);
        if (filter != null) {
            builder.filterExpression(filter);
        }

        return vectorStore.similaritySearch(builder.build());
    }

    @Override
    public String getName() {
        return "vector";
    }

    @Override
    public boolean isSupported() {
        return vectorStore != null;
    }
}