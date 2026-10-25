package com.afs.module.rag.search;

import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * 统一校验并构造 RAG 检索使用的元数据过滤条件。
 */
public final class RagSearchFilters {

    /** 支持的过滤字段白名单；caseType 是复合条件，会展开为 type=scamcase + category=值 */
    private static final Set<String> SUPPORTED_FILTERS = Set.of("type", "dbId", "category", "caseType");

    private RagSearchFilters() {
    }

    /**
     * 校验过滤字段是否在白名单内、dbId 值是否为整数；非法字段直接抛异常，防止注入向量过滤表达式。
     */
    public static void validate(Map<String, Object> filters) {
        if (filters == null) {
            return;
        }
        for (Map.Entry<String, Object> filter : filters.entrySet()) {
            if (filter.getKey() == null || !SUPPORTED_FILTERS.contains(filter.getKey())) {
                throw new IllegalArgumentException("不支持的检索过滤字段: " + filter.getKey());
            }
            if ("dbId".equals(filter.getKey()) && filter.getValue() != null) {
                try {
                    Long.parseLong(filter.getValue().toString());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("dbId 过滤值必须是整数", e);
                }
            }
        }
    }

    /**
     * 把过滤条件构造为向量库的 Filter.Expression（多个条件 AND 组合）；无有效条件时返回 null。
     */
    public static Filter.Expression buildVectorExpression(Map<String, Object> filters) {
        validate(filters);
        if (filters == null || filters.isEmpty()) {
            return null;
        }

        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        var operations = new ArrayList<FilterExpressionBuilder.Op>();
        for (Map.Entry<String, Object> filter : filters.entrySet()) {
            if (filter.getValue() == null) {
                continue;
            }
            if ("caseType".equals(filter.getKey())) {
                operations.add(builder.eq("type", "scamcase"));
                operations.add(builder.eq("category", filter.getValue().toString()));
            } else {
                operations.add(builder.eq(filter.getKey(), filter.getValue().toString()));
            }
        }
        return operations.stream()
                .reduce((left, right) -> builder.and(left, right))
                .map(FilterExpressionBuilder.Op::build)
                .orElse(null);
    }
}
