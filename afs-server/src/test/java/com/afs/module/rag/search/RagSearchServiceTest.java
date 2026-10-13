package com.afs.module.rag.search;

import com.afs.module.rag.rerank.RerankService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagSearchServiceTest {

    @Test
    void rerankUsesExpandedCandidatePoolButReturnsRerankedTopK() {
        SearchStrategyResolver strategyResolver = mock(SearchStrategyResolver.class);
        SearchStrategy strategy = mock(SearchStrategy.class);
        RerankService reRankService = mock(RerankService.class);
        RagSearchService ragSearchService = new RagSearchService(
                mock(VectorStore.class), strategyResolver, reRankService);
        List<Document> candidates = documents(20);
        List<Document> finalResults = candidates.subList(0, 7);

        when(strategyResolver.getStrategy()).thenReturn(strategy);
        when(strategy.getName()).thenReturn("hybrid");
        when(strategy.search("query", null, 20)).thenReturn(candidates);
        when(reRankService.rerank("query", candidates)).thenReturn(finalResults);
        ReflectionTestUtils.setField(ragSearchService, "rerankEnabled", true);
        ReflectionTestUtils.setField(ragSearchService, "rerankCandidateTopK", 20);

        List<Document> results = ragSearchService.searchRelevant("query", 5);

        assertEquals(5, results.size());
        verify(strategy).search("query", null, 20);
        verify(reRankService).rerank("query", candidates);
    }

    @Test
    void disabledRerankRetrievesOnlyRequestedTopK() {
        SearchStrategyResolver strategyResolver = mock(SearchStrategyResolver.class);
        SearchStrategy strategy = mock(SearchStrategy.class);
        RerankService reRankService = mock(RerankService.class);
        RagSearchService ragSearchService = new RagSearchService(
                mock(VectorStore.class), strategyResolver, reRankService);
        List<Document> results = documents(5);

        when(strategyResolver.getStrategy()).thenReturn(strategy);
        when(strategy.getName()).thenReturn("hybrid");
        when(strategy.search("query", null, 5)).thenReturn(results);
        ReflectionTestUtils.setField(ragSearchService, "rerankEnabled", false);
        ReflectionTestUtils.setField(ragSearchService, "rerankCandidateTopK", 20);

        assertEquals(results, ragSearchService.searchRelevant("query", 5));
        verify(strategy).search("query", null, 5);
        verify(reRankService, never()).rerank("query", results);
    }

    @Test
    void fallsBackToVectorStrategyWhenHybridSearchFails() {
        SearchStrategyResolver strategyResolver = mock(SearchStrategyResolver.class);
        SearchStrategy hybridStrategy = mock(SearchStrategy.class);
        SearchStrategy vectorStrategy = mock(SearchStrategy.class);
        RagSearchService ragSearchService = new RagSearchService(
                mock(VectorStore.class), strategyResolver, mock(RerankService.class));
        List<Document> fallbackResults = documents(2);

        when(strategyResolver.getStrategy()).thenReturn(hybridStrategy);
        when(strategyResolver.getStrategy("vector")).thenReturn(vectorStrategy);
        when(hybridStrategy.getName()).thenReturn("hybrid");
        when(hybridStrategy.search("query", null, 5)).thenThrow(new IllegalStateException("hybrid failed"));
        when(vectorStrategy.search("query", null, 5)).thenReturn(fallbackResults);

        assertEquals(fallbackResults, ragSearchService.searchRelevant("query", 5));
        verify(vectorStrategy).search("query", null, 5);
    }

    @Test
    void propagatesFailureWhenPrimaryAndFallbackSearchBothFail() {
        SearchStrategyResolver strategyResolver = mock(SearchStrategyResolver.class);
        SearchStrategy hybridStrategy = mock(SearchStrategy.class);
        SearchStrategy vectorStrategy = mock(SearchStrategy.class);
        RagSearchService ragSearchService = new RagSearchService(
                mock(VectorStore.class), strategyResolver, mock(RerankService.class));

        when(strategyResolver.getStrategy()).thenReturn(hybridStrategy);
        when(strategyResolver.getStrategy("vector")).thenReturn(vectorStrategy);
        when(hybridStrategy.getName()).thenReturn("hybrid");
        when(hybridStrategy.search("query", null, 5)).thenThrow(new IllegalStateException("hybrid failed"));
        when(vectorStrategy.search("query", null, 5)).thenThrow(new IllegalStateException("vector failed"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> ragSearchService.searchRelevant("query", 5));

        assertEquals("RAG 检索失败", exception.getMessage());
        assertEquals(1, exception.getCause().getSuppressed().length);
    }

    private static List<Document> documents(int count) {
        List<Document> documents = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            documents.add(Document.builder().id("chunk-" + i).text("document " + i).build());
        }
        return documents;
    }
}
