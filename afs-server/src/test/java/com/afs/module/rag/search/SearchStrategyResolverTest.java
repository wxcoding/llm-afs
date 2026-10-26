package com.afs.module.rag.search;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SearchStrategyResolver 单元测试。
 * 覆盖按配置选择混合/向量策略、混合策略缺失时回退向量、
 * 未知策略名显式失败、重名策略拒绝注册、不可用策略被跳过等分支。
 */
class SearchStrategyResolverTest {

    // 混合检索开关通过 ReflectionTestUtils 注入 @Value 字段
    private static SearchStrategyResolver resolverWith(boolean hybridEnabled, SearchStrategy... strategies) {
        SearchStrategyResolver resolver = new SearchStrategyResolver(List.of(strategies));
        ReflectionTestUtils.setField(resolver, "hybridSearchEnabled", hybridEnabled);
        return resolver;
    }

    private static SearchStrategy supportedStrategy(String name) {
        SearchStrategy strategy = mock(SearchStrategy.class);
        when(strategy.isSupported()).thenReturn(true);
        when(strategy.getName()).thenReturn(name);
        return strategy;
    }

    // 混合检索启用且策略存在时优先返回 hybrid
    @Test
    void returnsHybridStrategyWhenEnabledAndAvailable() {
        SearchStrategy vector = supportedStrategy("vector");
        SearchStrategy hybrid = supportedStrategy("hybrid");
        SearchStrategyResolver resolver = resolverWith(true, vector, hybrid);

        assertSame(hybrid, resolver.getStrategy());
    }

    // 混合检索未启用时默认返回 vector
    @Test
    void returnsVectorStrategyWhenHybridDisabled() {
        SearchStrategy vector = supportedStrategy("vector");
        SearchStrategy hybrid = supportedStrategy("hybrid");
        SearchStrategyResolver resolver = resolverWith(false, vector, hybrid);

        assertSame(vector, resolver.getStrategy());
    }

    // 混合检索启用但策略未注册时回退到 vector
    @Test
    void fallsBackToVectorWhenHybridEnabledButUnavailable() {
        SearchStrategy vector = supportedStrategy("vector");
        SearchStrategyResolver resolver = resolverWith(true, vector);

        assertSame(vector, resolver.getStrategy());
    }

    // 按名称获取不存在的策略时显式失败而非返回 null
    @Test
    void unknownStrategyNameThrowsIllegalState() {
        SearchStrategyResolver resolver = resolverWith(false, supportedStrategy("vector"));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> resolver.getStrategy("hybrid"));
        assertTrue(exception.getMessage().contains("hybrid"));
    }

    // 注册重名策略时构造阶段即失败
    @Test
    void duplicateStrategyNamesAreRejected() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> new SearchStrategyResolver(List.of(
                        supportedStrategy("vector"), supportedStrategy("vector"))));
        assertTrue(exception.getMessage().contains("vector"));
    }

    // isSupported=false 的策略不注册，也不出现在可用列表中
    @Test
    void unsupportedStrategiesAreSkipped() {
        SearchStrategy unsupported = mock(SearchStrategy.class);
        when(unsupported.isSupported()).thenReturn(false);
        SearchStrategyResolver resolver = resolverWith(false, supportedStrategy("vector"), unsupported);

        assertEquals(List.of("vector"), resolver.getAvailableStrategies());
    }

    // 可用策略列表包含全部已注册策略名
    @Test
    void availableStrategiesContainAllRegisteredNames() {
        SearchStrategyResolver resolver = resolverWith(false,
                supportedStrategy("vector"), supportedStrategy("hybrid"));

        List<String> names = resolver.getAvailableStrategies();
        assertEquals(2, names.size());
        assertTrue(names.contains("vector"));
        assertTrue(names.contains("hybrid"));
    }
}
