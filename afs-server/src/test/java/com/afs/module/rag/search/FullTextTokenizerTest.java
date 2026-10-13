package com.afs.module.rag.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FullTextTokenizerTest {

    @Test
    void tokenizesChineseRunsAsOverlappingBigramsAndKeepsLatinTerms() {
        assertEquals(
                List.of("电信", "信诈", "诈骗", "abc", "42"),
                FullTextTokenizer.tokenize("电信诈骗 ABC-42")
        );
    }

    @Test
    void retainsSingleCharacterChineseTermsAndReturnsEmptyForBlankText() {
        assertEquals(List.of("钱"), FullTextTokenizer.tokenize("钱"));
        assertEquals(List.of(), FullTextTokenizer.tokenize("  !!!  "));
    }
}
