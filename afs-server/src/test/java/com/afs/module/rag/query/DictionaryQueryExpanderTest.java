package com.afs.module.rag.query;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DictionaryQueryExpander 单元测试。
 * 词典为内置静态数据，无需 mock；覆盖空查询占位、未命中原样返回、
 * 同义词扩展、口语化转书面语、诈骗关键词通用扩展及调试信息输出。
 */
class DictionaryQueryExpanderTest {

    private final DictionaryQueryExpander expander = new DictionaryQueryExpander();

    // null 与空白查询返回单个空串占位，保证调用方拿到非空列表
    @Test
    void nullAndBlankQueryReturnSingleEmptyPlaceholder() {
        assertEquals(List.of(""), expander.expand(null));
        assertEquals(List.of(""), expander.expand("   "));
        assertEquals(List.of(""), expander.expand(""));
    }

    // 未命中任何词典词条时仅返回原始查询，不产生重复项
    @Test
    void queryWithoutDictionaryHitReturnsOriginalOnly() {
        List<String> result = expander.expand("今天天气如何");

        assertEquals(1, result.size());
        assertEquals("今天天气如何", result.get(0));
    }

    // 命中同义词时追加同义词扩展，并因含诈骗关键词追加通用扩展
    @Test
    void synonymHitAppendsSynonymsAndGeneralFraudExtension() {
        List<String> result = expander.expand("诈骗");

        assertEquals(3, result.size());
        // 第一项始终为原始查询
        assertEquals("诈骗", result.get(0));
        // 第二项为同义词扩展
        assertTrue(result.get(1).startsWith("诈骗 "));
        assertTrue(result.get(1).contains("骗局"));
        assertTrue(result.get(1).contains("欺诈"));
        // 第三项为诈骗关键词通用扩展
        assertTrue(result.get(2).contains("识别"));
        assertTrue(result.get(2).contains("防范"));
        assertTrue(result.get(2).contains("维权"));
    }

    // 口语化词汇先替换为书面语，再做同义词扩展
    @Test
    void colloquialPhraseIsConvertedToFormalWordingBeforeExpansion() {
        List<String> result = expander.expand("被骗了怎么办");

        assertEquals(3, result.size());
        assertEquals("被骗了怎么办", result.get(0));
        // "怎么办" 被替换为书面语，"被骗" 触发同义词扩展
        assertTrue(result.get(1).contains("如何处理"));
        assertTrue(result.get(1).contains("上当"));
        // 原查询含 "骗"，触发通用扩展
        assertTrue(result.get(2).contains("如何处理"));
        assertTrue(result.get(2).contains("防范"));
    }

    // 仅命中口语映射、无同义词与诈骗关键词时，结果只多出书面语版本
    @Test
    void colloquialOnlyConversionProducesOriginalPlusFormalQuery() {
        List<String> result = expander.expand("这事怎么办");

        assertEquals(2, result.size());
        assertEquals("这事怎么办", result.get(0));
        assertEquals("这事如何处理 解决方案", result.get(1));
    }

    // 词典统计输出核心词与口语映射数量
    @Test
    void dictionaryStatsReportEntryCounts() {
        String stats = expander.getDictionaryStats();

        assertTrue(stats.contains("23 个核心词"));
        assertTrue(stats.contains("15 个口语映射"));
    }

    // 调试信息包含原始查询与全部扩展结果
    @Test
    void expansionInfoListsOriginalAndExpandedQueries() {
        String info = expander.getExpansionInfo("你好");

        assertTrue(info.contains("原始查询: 你好"));
        assertTrue(info.contains("扩展查询数量: 1"));
        assertTrue(info.contains("[1] 你好"));
    }
}
