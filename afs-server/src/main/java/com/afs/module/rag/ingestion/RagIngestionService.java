package com.afs.module.rag.ingestion;

import com.afs.module.rag.search.FullTextTokenizer;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 负责 RAG 文档切分、向量化及全文索引的写入与维护。
 */
@RequiredArgsConstructor
@Service
public class RagIngestionService {

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
    private final TokenTextSplitter splitter = new TokenTextSplitter();

    @Transactional
    @CacheEvict(value = "vectorSearch", allEntries = true)
    public void addKnowledgeDocument(Long id, String title, String category, String content) {
        Document source = Document.builder()
                .text(content)
                .metadata("source", "知识库")
                .metadata("title", title)
                .metadata("category", category)
                .metadata("dbId", id.toString())
                .metadata("type", "knowledge")
                .build();
        indexDocument(source, "knowledge", id, title, category);
    }

    @Transactional
    @CacheEvict(value = "vectorSearch", allEntries = true)
    public void addScamCaseDocument(Long id, String title, String type, String content, String tips) {
        Document source = Document.builder()
                .text("案例经过：" + content + "\n防范提示：" + tips)
                .metadata("source", "诈骗案例")
                .metadata("title", title)
                .metadata("caseType", type)
                .metadata("category", type)
                .metadata("dbId", id.toString())
                .metadata("type", "scamcase")
                .build();
        indexDocument(source, "scamcase", id, title, type);
    }

    private void indexDocument(Document source, String sourceType, Long sourceId, String title, String category) {
        deleteDocumentIndex(sourceType, sourceId);
        List<Document> splitChunks = splitter.apply(List.of(source));
        List<Document> chunks = new ArrayList<>(splitChunks.size());
        for (int i = 0; i < splitChunks.size(); i++) {
            Document splitChunk = splitChunks.get(i);
            String chunkId = UUID.nameUUIDFromBytes(
                    (sourceType + ":" + sourceId + ":" + i).getBytes(StandardCharsets.UTF_8)).toString();
            chunks.add(Document.builder()
                    .id(chunkId)
                    .text(splitChunk.getText())
                    .metadata(splitChunk.getMetadata())
                    .build());
        }

        if (chunks.isEmpty()) {
            return;
        }

        vectorStore.add(chunks);
        jdbcTemplate.batchUpdate(
                "INSERT INTO rag_chunk "
                        + "(chunk_id, source_type, source_id, title, category, content, search_text) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int index) throws SQLException {
                        Document chunk = chunks.get(index);
                        List<String> terms = FullTextTokenizer.tokenize(
                                title + " " + category + " " + chunk.getText());
                        ps.setString(1, chunk.getId());
                        ps.setString(2, sourceType);
                        ps.setLong(3, sourceId);
                        ps.setString(4, title);
                        ps.setString(5, category);
                        ps.setString(6, chunk.getText());
                        ps.setString(7, String.join(" ", terms));
                    }

                    @Override
                    public int getBatchSize() {
                        return chunks.size();
                    }
                });
    }

    private void deleteDocumentIndex(String sourceType, Long sourceId) {
        var filter = filterBuilder.and(
                filterBuilder.eq("dbId", sourceId.toString()),
                filterBuilder.eq("type", sourceType)
        ).build();
        vectorStore.delete(filter);
        jdbcTemplate.update(
                "DELETE FROM rag_chunk WHERE source_type = ? AND source_id = ?",
                sourceType, sourceId);
    }

    @Transactional
    @CacheEvict(value = "vectorSearch", allEntries = true)
    public void deleteKnowledgeDocument(Long id) {
        deleteDocumentIndex("knowledge", id);
    }

    @Transactional
    @CacheEvict(value = "vectorSearch", allEntries = true)
    public void deleteScamCaseDocument(Long id) {
        deleteDocumentIndex("scamcase", id);
    }

    @Transactional
    @CacheEvict(value = "vectorSearch", allEntries = true)
    public void clearKnowledgeIndex() {
        vectorStore.delete(filterBuilder.eq("type", "knowledge").build());
        jdbcTemplate.update("DELETE FROM rag_chunk WHERE source_type = 'knowledge'");
    }

    @Transactional
    @CacheEvict(value = "vectorSearch", allEntries = true)
    public void clearScamCaseIndex() {
        vectorStore.delete(filterBuilder.eq("type", "scamcase").build());
        jdbcTemplate.update("DELETE FROM rag_chunk WHERE source_type = 'scamcase'");
    }

    @Transactional
    @CacheEvict(value = "vectorSearch", allEntries = true)
    public void clearAllIndexes() {
        jdbcTemplate.update("TRUNCATE vector_store");
        jdbcTemplate.update("TRUNCATE rag_chunk");
    }

    @Transactional
    @CacheEvict(value = "vectorSearch", allEntries = true)
    public void deleteByType(String type) {
        vectorStore.delete(filterBuilder.eq("type", type).build());
        jdbcTemplate.update("DELETE FROM rag_chunk WHERE source_type = ?", type);
    }
}
