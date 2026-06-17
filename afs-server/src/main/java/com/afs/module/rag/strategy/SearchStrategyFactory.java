package com.afs.module.rag.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 检索策略工厂
 * 
 * 负责管理和获取不同的检索策略
 * 
 * 使用方式：
 * <pre>
 * SearchStrategy strategy = factory.getStrategy();
 * List<Document> results = strategy.search(query, filters, topK);
 * </pre>
 * 
 * @author AFS
 */
@Slf4j
@Component
public class SearchStrategyFactory {

    private final Map<String, SearchStrategy> strategyMap = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private VectorSearchStrategy vectorSearchStrategy;

    @Autowired(required = false)
    private HybridSearchStrategy hybridSearchStrategy;

    @Value("${spring.ai.vectorstore.hybrid-search.enabled:false}")
    private boolean hybridSearchEnabled;

    /**
     * 初始化策略映射
     */
    @Autowired(required = false)
    public void initStrategies() {
        if (vectorSearchStrategy != null && vectorSearchStrategy.isSupported()) {
            strategyMap.put(vectorSearchStrategy.getName(), vectorSearchStrategy);
            log.info("注册检索策略: {}", vectorSearchStrategy.getName());
        }
        
        if (hybridSearchStrategy != null && hybridSearchStrategy.isSupported()) {
            strategyMap.put(hybridSearchStrategy.getName(), hybridSearchStrategy);
            log.info("注册检索策略: {}", hybridSearchStrategy.getName());
        }
    }

    /**
     * 获取当前配置的检索策略
     * 
     * @return 当前策略实例
     */
    public SearchStrategy getStrategy() {
        if (hybridSearchEnabled && hybridSearchStrategy != null && hybridSearchStrategy.isSupported()) {
            return hybridSearchStrategy;
        }
        
        // 默认返回向量检索策略
        if (vectorSearchStrategy != null && vectorSearchStrategy.isSupported()) {
            return vectorSearchStrategy;
        }
        
        throw new IllegalStateException("未找到可用的检索策略");
    }

    /**
     * 根据名称获取策略
     * 
     * @param name 策略名称
     * @return 策略实例，如果不存在返回 null
     */
    public SearchStrategy getStrategy(String name) {
        return strategyMap.get(name);
    }

    /**
     * 获取所有可用策略
     * 
     * @return 可用策略列表
     */
    public List<String> getAvailableStrategies() {
        return strategyMap.keySet().stream().toList();
    }

    /**
     * 判断是否支持指定策略
     * 
     * @param name 策略名称
     * @return true 如果支持
     */
    public boolean isStrategySupported(String name) {
        SearchStrategy strategy = strategyMap.get(name);
        return strategy != null && strategy.isSupported();
    }
}