package com.afs.module.rag.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 混合检索策略
 * 
 * 结合 BM25 关键词检索和向量语义检索，通过 RRF 算法融合结果
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
 * @author AFS
 */
@Slf4j
@Component("hybridSearchStrategy")
public class HybridSearchStrategy implements SearchStrategy {

    @Autowired(required = false)
    private com.afs.module.rag.base.HybridSearchService hybridSearchService;

    @Value("${spring.ai.vectorstore.hybrid-search.enabled:false}")
    private boolean hybridSearchEnabled;

    @Override
    public List<Document> search(String query, Map<String, Object> filters, int topK) {
        log.debug("使用混合检索策略，查询词: {}, topK: {}", query, topK);
        
        if (hybridSearchService == null) {
            log.warn("混合检索服务未配置，降级为纯向量检索");
            throw new IllegalStateException("混合检索服务未配置");
        }

        List<Document> results = hybridSearchService.search(query, filters);
        
        // 限制返回数量
        if (results.size() > topK) {
            return results.subList(0, topK);
        }
        
        return results;
    }

    @Override
    public String getName() {
        return "hybrid";
    }

    @Override
    public boolean isSupported() {
        return hybridSearchEnabled && hybridSearchService != null;
    }
}