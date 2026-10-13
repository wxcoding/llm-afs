package com.afs.module.rag.search;

import com.afs.module.rag.query.DictionaryQueryExpander;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostgresHybridSearchServiceTest {

    @Test
    void fusesSharedChunkIdsAndLoadsResultsFromChunkIndex() {
        VectorStore vectorStore = mock(VectorStore.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        PostgresHybridSearchService service = new PostgresHybridSearchService(
                vectorStore, jdbcTemplate, new DictionaryQueryExpander()
        );
        ReflectionTestUtils.setField(service, "hybridSearchEnabled", true);
        ReflectionTestUtils.setField(service, "queryExpansionEnabled", false);
        ReflectionTestUtils.setField(service, "vectorTopK", 5);
        ReflectionTestUtils.setField(service, "keywordTopK", 5);
        ReflectionTestUtils.setField(service, "maxExpansionQueries", 5);

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                document("shared"), document("vector-only")
        ));
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.startsWith("SELECT c.chunk_id")) {
                assertEquals("'诈骗'", invocation.getArgument(1));
                assertEquals("'诈骗'", invocation.getArgument(2));
                assertTrue(sql.contains("c.search_vector @@ to_tsquery('simple'"));
                assertTrue(sql.contains("ts_rank_cd"));
                return List.of(
                        Map.of("chunk_id", "shared", "score", 1.0),
                        Map.of("chunk_id", "keyword-only", "score", 0.5)
                );
            }
            return List.of(
                    chunkRow("shared"),
                    chunkRow("vector-only"),
                    chunkRow("keyword-only")
            );
        });

        List<Document> results = service.search("诈骗", null, 3);

        assertEquals(List.of("shared", "keyword-only", "vector-only"),
                results.stream().map(Document::getId).toList());
        assertEquals("1", results.get(0).getMetadata().get("dbId"));
    }

    @Test
    void queryExpansionDoesNotAddMultipleRrfContributionsForSameChunk() {
        VectorStore vectorStore = mock(VectorStore.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DictionaryQueryExpander queryExpander = mock(DictionaryQueryExpander.class);
        PostgresHybridSearchService service = new PostgresHybridSearchService(vectorStore, jdbcTemplate, queryExpander);
        ReflectionTestUtils.setField(service, "hybridSearchEnabled", true);
        ReflectionTestUtils.setField(service, "queryExpansionEnabled", true);
        ReflectionTestUtils.setField(service, "vectorTopK", 5);
        ReflectionTestUtils.setField(service, "keywordTopK", 5);
        ReflectionTestUtils.setField(service, "maxExpansionQueries", 2);

        when(queryExpander.expand("诈骗")).thenReturn(List.of("诈骗", "骗钱"));
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(document("a-vector")));
        AtomicInteger fullTextQueries = new AtomicInteger();
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.startsWith("SELECT c.chunk_id")) {
                fullTextQueries.incrementAndGet();
                return List.of(Map.of("chunk_id", "z-keyword", "score", 1.0));
            }
            return List.of(chunkRow("a-vector"), chunkRow("z-keyword"));
        });

        List<Document> results = service.search("诈骗", null, 2);

        assertEquals(2, fullTextQueries.get());
        assertEquals(List.of("a-vector", "z-keyword"), results.stream().map(Document::getId).toList());
    }

    @Test
    void reportsFailureWhenBothRecallChannelsFail() {
        VectorStore vectorStore = mock(VectorStore.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        PostgresHybridSearchService service = new PostgresHybridSearchService(
                vectorStore, jdbcTemplate, new DictionaryQueryExpander()
        );
        ReflectionTestUtils.setField(service, "hybridSearchEnabled", true);
        ReflectionTestUtils.setField(service, "queryExpansionEnabled", false);
        ReflectionTestUtils.setField(service, "vectorTopK", 5);
        ReflectionTestUtils.setField(service, "keywordTopK", 5);
        ReflectionTestUtils.setField(service, "maxExpansionQueries", 5);

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new IllegalStateException("vector unavailable"));
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                .thenThrow(new IllegalStateException("full text unavailable"));

        assertThrows(IllegalStateException.class, () -> service.search("诈骗", null, 5));
    }

    private static Document document(String id) {
        return Document.builder().id(id).text("text").build();
    }

    private static Map<String, Object> chunkRow(String id) {
        return Map.of(
                "chunk_id", id,
                "source_type", "knowledge",
                "source_id", 1L,
                "title", "Anti-fraud guidance",
                "category", "prevention",
                "content", "Knowledge chunk " + id
        );
    }
}
