package com.afs.module.rag.search;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HybridSearchStrategy 单元测试。
 * 覆盖对 PostgresHybridSearchService 的委托与过滤条件透传、超出 topK 的截断、
 * 服务异常的直接传播，以及 isSupported 随开关与服务可用性变化的分支。
 */
class HybridSearchStrategyTest {

    private static List<Document> documents(int count) {
        List<Document> documents = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            documents.add(Document.builder().id("chunk-" + i).text("text " + i).build());
        }
        return documents;
    }

    // 检索请求连同过滤条件原样透传给混合检索服务
    @Test
    void delegatesSearchToHybridServiceWithFilters() {
        PostgresHybridSearchService hybridSearchService = mock(PostgresHybridSearchService.class);
        HybridSearchStrategy strategy = new HybridSearchStrategy(hybridSearchService);
        Map<String, Object> filters = Map.of("category", "贷款");
        List<Document> results = documents(2);
        when(hybridSearchService.search("贷款套路", filters, 5)).thenReturn(results);

        List<Document> returned = strategy.search("贷款套路", filters, 5);

        assertSame(results, returned);
        verify(hybridSearchService).search("贷款套路", filters, 5);
    }

    // 服务返回数量超过 topK 时截断为前 topK 条
    @Test
    void truncatesResultsWhenServiceReturnsMoreThanTopK() {
        PostgresHybridSearchService hybridSearchService = mock(PostgresHybridSearchService.class);
        HybridSearchStrategy strategy = new HybridSearchStrategy(hybridSearchService);
        when(hybridSearchService.search("诈骗", null, 4)).thenReturn(documents(6));

        List<Document> returned = strategy.search("诈骗", null, 4);

        assertEquals(4, returned.size());
        assertEquals(List.of("chunk-0", "chunk-1", "chunk-2", "chunk-3"),
                returned.stream().map(Document::getId).toList());
    }

    // 服务无命中时返回空列表
    @Test
    void returnsEmptyListWhenServiceFindsNothing() {
        PostgresHybridSearchService hybridSearchService = mock(PostgresHybridSearchService.class);
        HybridSearchStrategy strategy = new HybridSearchStrategy(hybridSearchService);
        List<Document> empty = List.of();
        when(hybridSearchService.search("诈骗", null, 5)).thenReturn(empty);

        assertSame(empty, strategy.search("诈骗", null, 5));
    }

    // 服务抛错时异常直接传播，由上层 RagSearchService 负责降级
    @Test
    void propagatesHybridServiceFailure() {
        PostgresHybridSearchService hybridSearchService = mock(PostgresHybridSearchService.class);
        HybridSearchStrategy strategy = new HybridSearchStrategy(hybridSearchService);
        when(hybridSearchService.search("诈骗", null, 5))
                .thenThrow(new IllegalStateException("检索通道发生错误且没有可用召回结果"));

        assertThrows(IllegalStateException.class, () -> strategy.search("诈骗", null, 5));
    }

    // 策略名固定为 hybrid
    @Test
    void strategyNameIsHybrid() {
        HybridSearchStrategy strategy = new HybridSearchStrategy(mock(PostgresHybridSearchService.class));

        assertEquals("hybrid", strategy.getName());
    }

    // 仅在开关启用且服务存在时才声明可用
    @Test
    void supportedOnlyWhenEnabledAndServicePresent() {
        HybridSearchStrategy strategy = new HybridSearchStrategy(mock(PostgresHybridSearchService.class));

        ReflectionTestUtils.setField(strategy, "hybridSearchEnabled", true);
        assertTrue(strategy.isSupported());

        ReflectionTestUtils.setField(strategy, "hybridSearchEnabled", false);
        assertFalse(strategy.isSupported());

        // 服务为 null 时即使开关启用也不可用
        HybridSearchStrategy withoutService = new HybridSearchStrategy(null);
        ReflectionTestUtils.setField(withoutService, "hybridSearchEnabled", true);
        assertFalse(withoutService.isSupported());
    }
}
