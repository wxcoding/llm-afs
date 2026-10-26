package com.afs.module.rag.search;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 混合检索策略
 * 
 * 结合 PostgreSQL 全文关键词检索和向量语义检索，通过 RRF 算法融合结果
 * 
 * 解决的问题：
 * 1. 用户用词与文档用词不一致时，纯向量检索效果差
 * 2. 纯关键词检索无法理解语义
 * 
 * 实现原理：
 * 1. 全文检索：根据关键词匹配度和 ts_rank_cd 计算相关性分数
 * 2. 向量检索：根据语义相似度计算相关性分数
 * 3. RRF 融合：将两种检索结果按排名融合
 * 
 * @author guanwx
 */
@Slf4j
@RequiredArgsConstructor
@Component("hybridSearchStrategy")
public class HybridSearchStrategy implements SearchStrategy {

    /** PostgreSQL 混合检索服务，融合全文与向量两路召回 */
    private final PostgresHybridSearchService hybridSearchService;

    /** 是否启用混合检索 */
    @Value("${spring.ai.vectorstore.hybrid-search.enabled:false}")
    private boolean hybridSearchEnabled;

    /**
     * 执行混合检索：委托给 PostgreSQL 混合检索服务，融合全文与向量两路召回。
     *
     * @param query   用户查询词
     * @param filters 过滤条件（可选）
     * @param topK    返回结果数量
     * @return 相关文档列表
     */
    @Override
    public List<Document> search(String query, Map<String, Object> filters, int topK) {
        log.debug("使用混合检索策略，查询词: {}, topK: {}", query, topK);

        // topK 由上游决定：未重排时是最终返回数，启用重排时是扩大的候选池大小。
        List<Document> results = hybridSearchService.search(query, filters, topK);

        // 防御性截断：融合结果理论上不超过 topK，此处兜底
        if (results.size() > topK) {
            return results.subList(0, topK);
        }

        return results;
    }

    /**
     * 返回策略名称。
     *
     * @return 策略名称 "hybrid"
     */
    @Override
    public String getName() {
        return "hybrid";
    }

    /**
     * 仅当配置启用混合检索且服务可用时才支持该策略。
     */
    @Override
    public boolean isSupported() {
        return hybridSearchEnabled && hybridSearchService != null;
    }
}