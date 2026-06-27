package com.afs.module.rag.strategy;

import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

/**
 * 检索策略接口
 * 
 * 定义检索策略的统一接口，支持不同的检索算法实现
 * 
 * 1. 开闭原则：新增策略无需修改现有代码
 * 2. 单一职责：每个策略只关注一种检索方式
 * 3. 解耦合：策略实现与调用方解耦
 *
 */
public interface SearchStrategy {
    
    /**
     * 执行检索
     * 
     * @param query 用户查询词
     * @param filters 过滤条件（可选）
     * @param topK 返回结果数量
     * @return 相关文档列表
     */
    List<Document> search(String query, Map<String, Object> filters, int topK);
    
    /**
     * 获取策略名称
     * 
     * @return 策略名称
     */
    String getName();
    
    /**
     * 是否支持该策略
     * 
     * @return true 如果支持，false 否则
     */
    boolean isSupported();
}