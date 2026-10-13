package com.afs.module.rag.search;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class VectorSearchStrategyTest {

    @Test
    void appliesAllFiltersIncludingCaseTypeMapping() {
        VectorStore vectorStore = mock(VectorStore.class);
        VectorSearchStrategy strategy = new VectorSearchStrategy(vectorStore);

        strategy.search("诈骗", Map.of("dbId", 12, "caseType", "网购诈骗"), 5);

        var request = org.mockito.ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(request.capture());
        assertNotNull(request.getValue().getFilterExpression());
    }

    @Test
    void rejectsInvalidFiltersAndTopK() {
        VectorSearchStrategy strategy = new VectorSearchStrategy(mock(VectorStore.class));

        assertThrows(IllegalArgumentException.class,
                () -> strategy.search("诈骗", Map.of("unknown", "value"), 5));
        assertThrows(IllegalArgumentException.class,
                () -> strategy.search("诈骗", Map.of(), 0));
    }
}
