package com.afs.db;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class DatabaseMigrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("pgvector/pgvector:pg17")
                    .withDatabaseName("afs")
                    .withUsername("afs")
                    .withPassword("afs");

    @Test
    void allMigrationsApplyToFreshPostgresWithPgvector() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE EXTENSION IF NOT EXISTS vector");

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        String chunkId = UUID.randomUUID().toString();
        jdbc.update(
                """
                        INSERT INTO rag_chunk (chunk_id, source_type, source_id, title, content, search_text)
                        VALUES (?, 'knowledge', 1, '反诈测试', '退款诈骗测试内容', 'refund phishing')
                        """, chunkId);
        Integer fullTextMatches = jdbc.queryForObject(
                "SELECT COUNT(*) FROM rag_chunk WHERE search_vector @@ plainto_tsquery('simple', 'phishing')",
                Integer.class);
        jdbc.execute("CREATE TEMP TABLE vector_smoke_test (embedding vector(3))");
        jdbc.update("INSERT INTO vector_smoke_test (embedding) VALUES (?::vector)", "[1,0,0]");
        Double vectorDistance = jdbc.queryForObject(
                "SELECT embedding <=> '[1,0,0]'::vector FROM vector_smoke_test", Double.class);

        Integer appliedMigrations = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE", Integer.class);
        Integer vectorExtensionCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pg_extension WHERE extname = 'vector'", Integer.class);
        Integer degradedColumnCount = jdbc.queryForObject(
                """
                        SELECT COUNT(*) FROM information_schema.columns
                        WHERE table_name = 'chat_message' AND column_name = 'degraded'
                        """, Integer.class);
        Integer searchVectorColumnCount = jdbc.queryForObject(
                """
                        SELECT COUNT(*) FROM information_schema.columns
                        WHERE table_name = 'rag_chunk' AND column_name = 'search_vector'
                          AND udt_name = 'tsvector'
                        """, Integer.class);

        assertEquals(18, appliedMigrations);
        assertTrue(vectorExtensionCount != null && vectorExtensionCount > 0);
        assertEquals(1, degradedColumnCount);
        assertEquals(1, searchVectorColumnCount);
        assertEquals(1, fullTextMatches);
        assertEquals(0.0, vectorDistance, 1.0e-9);
    }
}
