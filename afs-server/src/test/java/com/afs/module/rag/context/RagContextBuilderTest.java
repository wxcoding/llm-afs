package com.afs.module.rag.context;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RagContextBuilderTest {

    @Test
    void formatsSourcesWithOptionalTitles() {
        RagContextBuilder builder = new RagContextBuilder();

        String context = builder.buildContext(List.of(
                Map.of("source", "知识库", "title", "防骗指南", "content", "不要向陌生账户转账"),
                Map.of("source", "诈骗案例", "content", "受害人向陌生账户转账")
        ));

        assertEquals("【知识库 - 防骗指南】\n不要向陌生账户转账\n\n【诈骗案例】\n受害人向陌生账户转账", context);
    }
}
