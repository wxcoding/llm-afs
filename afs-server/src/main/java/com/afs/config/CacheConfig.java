package com.afs.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置类
 * 
 * 最佳实践：
 * 1. 使用 Caffeine 作为本地缓存（高性能）
 * 2. 配置合理的过期策略
 * 3. 限制缓存大小防止内存溢出
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 向量搜索结果缓存
     * 
     * 配置说明：
     * - maximumSize: 最大缓存条目数（防止OOM）
     * - expireAfterWrite: 写入后10分钟过期（平衡实时性和性能）
     * - recordStats: 记录缓存命中率用于监控
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("vectorSearch");
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)                          // 最多缓存1000个查询结果
            .expireAfterWrite(10, TimeUnit.MINUTES)     // 10分钟后过期
            .recordStats()                              // 记录统计信息
            .initialCapacity(100));                     // 初始容量
        
        return cacheManager;
    }
}
