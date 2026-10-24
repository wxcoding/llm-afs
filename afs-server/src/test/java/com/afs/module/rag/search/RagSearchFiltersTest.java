package com.afs.module.rag.search;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RagSearchFilters 单元测试。
 * 覆盖过滤字段白名单校验（含 dbId 数值约束）与向量过滤表达式构造：
 * 单条件等值、多条件 AND 组合、caseType 到 type/category 的映射、空值跳过。
 */
class RagSearchFiltersTest {

    // null 与全部受支持字段均可通过校验
    @Test
    void validateAcceptsNullAndAllSupportedFields() {
        assertDoesNotThrow(() -> RagSearchFilters.validate(null));
        assertDoesNotThrow(() -> RagSearchFilters.validate(
                Map.of("type", "knowledge", "dbId", "12", "category", "prevention", "caseType", "刷单诈骗")));
    }

    // 白名单外的字段直接拒绝
    @Test
    void validateRejectsUnsupportedFieldName() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> RagSearchFilters.validate(Map.of("region", "华东")));
        assertTrue(exception.getMessage().contains("region"));
    }

    // null 字段名同样拒绝（HashMap 允许 null key）
    @Test
    void validateRejectsNullFieldName() {
        Map<String, Object> filters = new HashMap<>();
        filters.put(null, "value");

        assertThrows(IllegalArgumentException.class, () -> RagSearchFilters.validate(filters));
    }

    // dbId 必须是可解析的整数，非数值抛出带说明的异常
    @Test
    void validateRejectsNonNumericDbId() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> RagSearchFilters.validate(Map.of("dbId", "abc")));
        assertTrue(exception.getMessage().contains("dbId"));

        assertDoesNotThrow(() -> RagSearchFilters.validate(Map.of("dbId", 12L)));
    }

    // null 或空过滤条件不构造表达式，交由下游无过滤检索
    @Test
    void buildVectorExpressionReturnsNullForNullOrEmptyFilters() {
        assertNull(RagSearchFilters.buildVectorExpression(null));
        assertNull(RagSearchFilters.buildVectorExpression(Map.of()));
    }

    // 单个过滤条件生成等值表达式
    @Test
    void buildVectorExpressionBuildsEqualityForSingleFilter() {
        Filter.Expression expression = RagSearchFilters.buildVectorExpression(Map.of("type", "knowledge"));

        assertNotNull(expression);
        assertEqExpression(expression, "type", "knowledge");
    }

    // 多个过滤条件以 AND 组合（LinkedHashMap 保证遍历顺序）
    @Test
    void buildVectorExpressionCombinesMultipleFiltersWithAnd() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("type", "knowledge");
        filters.put("dbId", "12");

        Filter.Expression expression = RagSearchFilters.buildVectorExpression(filters);

        assertNotNull(expression);
        assertEquals(Filter.ExpressionType.AND, expression.type());
        assertEqExpression((Filter.Expression) expression.left(), "type", "knowledge");
        assertEqExpression((Filter.Expression) expression.right(), "dbId", "12");
    }

    // caseType 映射为 type=scamcase 与 category=案例类型 的 AND 组合
    @Test
    void caseTypeFilterMapsToScamcaseTypeAndCategory() {
        Filter.Expression expression = RagSearchFilters.buildVectorExpression(
                Map.of("caseType", "网购诈骗"));

        assertNotNull(expression);
        assertEquals(Filter.ExpressionType.AND, expression.type());
        assertEqExpression((Filter.Expression) expression.left(), "type", "scamcase");
        assertEqExpression((Filter.Expression) expression.right(), "category", "网购诈骗");
    }

    // null 值条件被跳过；全部为 null 时不构造表达式
    @Test
    void buildVectorExpressionSkipsNullValues() {
        Map<String, Object> allNull = new HashMap<>();
        allNull.put("type", null);
        assertNull(RagSearchFilters.buildVectorExpression(allNull));

        Map<String, Object> partiallyNull = new LinkedHashMap<>();
        partiallyNull.put("type", "knowledge");
        partiallyNull.put("dbId", null);
        Filter.Expression expression = RagSearchFilters.buildVectorExpression(partiallyNull);
        assertNotNull(expression);
        assertEqExpression(expression, "type", "knowledge");
    }

    // 构造表达式前先做白名单校验，非法字段直接抛出
    @Test
    void buildVectorExpressionValidatesFiltersFirst() {
        assertThrows(IllegalArgumentException.class,
                () -> RagSearchFilters.buildVectorExpression(Map.of("unknown", "value")));
    }

    private static void assertEqExpression(Filter.Expression expression, String key, Object value) {
        assertEquals(Filter.ExpressionType.EQ, expression.type());
        assertEquals(key, ((Filter.Key) expression.left()).key());
        assertEquals(value, ((Filter.Value) expression.right()).value());
    }
}
