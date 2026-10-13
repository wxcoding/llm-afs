package com.afs.module.rag.rerank;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RerankServiceTest {

    @Test
    void missingProviderCredentialsPreserveCandidatesWithoutMockReranking() {
        RerankService service = new RerankService();
        List<Document> candidates = List.of(
                Document.builder().id("first").text("unrelated").build(),
                Document.builder().id("second").text("query").build()
        );
        ReflectionTestUtils.setField(service, "rerankEnabled", true);
        ReflectionTestUtils.setField(service, "rerankProvider", "jina");
        ReflectionTestUtils.setField(service, "jinaApiKey", "");
        ReflectionTestUtils.setField(service, "rerankTopN", 1);

        List<Document> results = service.rerank("query", candidates);

        assertEquals(candidates, results);
    }

    @Test
    void unknownProviderPreservesCandidatesInsteadOfFallingBackToMock() {
        RerankService service = new RerankService();
        List<Document> candidates = List.of(
                Document.builder().id("first").text("unrelated").build(),
                Document.builder().id("second").text("query").build()
        );
        ReflectionTestUtils.setField(service, "rerankEnabled", true);
        ReflectionTestUtils.setField(service, "rerankProvider", "unknown");
        ReflectionTestUtils.setField(service, "rerankTopN", 1);

        List<Document> results = service.rerank("query", candidates);

        assertEquals(candidates, results);
    }
}
