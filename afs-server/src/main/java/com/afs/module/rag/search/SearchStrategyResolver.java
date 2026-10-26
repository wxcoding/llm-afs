package com.afs.module.rag.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 根据配置解析当前可用的检索策略。
 * @author guanwx
 */
@Slf4j
@Component
public class SearchStrategyResolver {

    /** 已注册的可用检索策略（策略名 → 策略实例） */
    private final Map<String, SearchStrategy> strategyMap;

    /** 是否启用混合检索 */
    @Value("${spring.ai.vectorstore.hybrid-search.enabled:false}")
    private boolean hybridSearchEnabled;

    /**
     * 收集容器中所有检索策略，仅注册当前可用的；同名策略视为配置错误，直接启动失败。
     */
    public SearchStrategyResolver(List<SearchStrategy> strategies) {
        Map<String, SearchStrategy> registered = new LinkedHashMap<>();
        for (SearchStrategy strategy : strategies) {
            if (!strategy.isSupported()) {
                continue;
            }
            SearchStrategy previous = registered.putIfAbsent(strategy.getName(), strategy);
            if (previous != null) {
                throw new IllegalStateException("存在重复的检索策略名称: " + strategy.getName());
            }
            log.info("注册检索策略: {}", strategy.getName());
        }
        strategyMap = Map.copyOf(registered);
    }

    /**
     * 获取当前配置的策略；混合策略不可用时保留纯向量回退。
     */
    public SearchStrategy getStrategy() {
        if (hybridSearchEnabled) {
            SearchStrategy hybrid = strategyMap.get("hybrid");
            if (hybrid != null) {
                return hybrid;
            }
            log.warn("混合检索已启用但策略不可用，回退到纯向量检索");
        }
        return getStrategy("vector");
    }

    /**
     * 按名称获取策略；不存在时显式失败，避免调用方空指针或伪装成空结果。
     */
    public SearchStrategy getStrategy(String name) {
        SearchStrategy strategy = strategyMap.get(name);
        if (strategy == null) {
            throw new IllegalStateException("检索策略不可用: " + name);
        }
        return strategy;
    }

    /**
     * 获取所有可用策略
     *
     * @return 可用策略列表
     */
    public List<String> getAvailableStrategies() {
        return strategyMap.keySet().stream().toList();
    }
}