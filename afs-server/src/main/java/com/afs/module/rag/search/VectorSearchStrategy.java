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
 * @author guanwx
 */
@RequiredArgsConstructor
@Slf4j
@Component("vectorSearchStrategy")
public class VectorSearchStrategy implements SearchStrategy {

    /** 向量库，执行语义相似度检索 */
    private final VectorStore vectorStore;

    /**
     * 执行纯向量相似度检索，按相似度阈值 0.5 过滤明显不相关的结果。
     *
     * @param query   用户查询词，不能为空
     * @param filters 过滤条件（可选）
     * @param topK    返回结果数量，必须大于 0
     * @return 相关文档列表
     */
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

        // 相似度阈值 0.5：过滤掉明显不相关的结果，避免低质量上下文进入提示词
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

    /**
     * 返回策略名称。
     *
     * @return 策略名称 "vector"
     */
    @Override
    public String getName() {
        return "vector";
    }

    /**
     * 向量库可用即支持该策略（兜底策略，始终应可用）。
     */
    @Override
    public boolean isSupported() {
        return vectorStore != null;
    }
}