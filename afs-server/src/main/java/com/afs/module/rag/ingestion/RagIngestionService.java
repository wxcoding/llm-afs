package com.afs.module.rag.ingestion;

import com.afs.module.rag.search.FullTextTokenizer;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
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
    // 中文句子级切分：按句读切分、贪心合并到 400 字、块间重叠 60 字，避免在句中硬切
    private final TextSplitter splitter = new ChineseSentenceTextSplitter();
//    private final TokenTextSplitter splitter = new TokenTextSplitter();

    /**
     * 将知识库条目写入向量索引与全文索引（已存在时先删后建，幂等重建）。
     *
     * 由 RagIngestionProcessor 在独立线程池中异步调用；
     * 失败由调用方记录状态，检索端短暂不可见新文档。
     *
     * @param id       知识库记录主键
     * @param title    条目标题，同时参与全文检索词项构造
     * @param category 条目分类，用于检索过滤
     * @param content  条目正文，即实际被切分和向量化的内容
     */
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

    /**
     * 将诈骗案例写入向量索引与全文索引（已存在时先删后建，幂等重建）。
     *
     * 由 RagIngestionProcessor 在独立线程池中异步调用。
     *
     * @param id      案例记录主键
     * @param title   案例标题
     * @param type    案例类型，同时作为检索分类（category）
     * @param content 案例经过
     * @param tips    防范提示，与案例经过拼接为同一篇文档后统一切分
     */
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

    /**
     * 单篇文档的索引主流程：先清除旧索引，再切分、向量化并双写 rag_chunk 全文表。
     */
    private void indexDocument(Document source, String sourceType, Long sourceId, String title, String category) {
        // 先删后建：同一文档重复入库时不会产生残留 chunk
        deleteDocumentIndex(sourceType, sourceId);
        List<Document> splitChunks = splitter.apply(List.of(source));
        List<Document> chunks = new ArrayList<>(splitChunks.size());
        for (int i = 0; i < splitChunks.size(); i++) {
            Document splitChunk = splitChunks.get(i);
            // chunk_id 由 sourceType:sourceId:序号 确定性生成，重建索引时覆盖同一批 ID，天然幂等
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

        // 双写：vector_store 供向量召回，rag_chunk 供 PostgreSQL 全文召回，两路以 chunk_id 关联
        vectorStore.add(chunks);
        jdbcTemplate.batchUpdate(
                "INSERT INTO rag_chunk "
                        + "(chunk_id, source_type, source_id, title, category, content, search_text) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int index) throws SQLException {
                        Document chunk = chunks.get(index);
                        // 标题与分类也参与分词，提升关键词通道对短正文的召回能力
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

    /**
     * 按来源删除某篇文档的全部索引（向量库按元数据过滤删除，全文表按主键删除）。
     */
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

    /**
     * 删除指定知识库条目的全部索引。
     */
    @Transactional
    @CacheEvict(value = "vectorSearch", allEntries = true)
    public void deleteKnowledgeDocument(Long id) {
        deleteDocumentIndex("knowledge", id);
    }

    /**
     * 删除指定诈骗案例的全部索引。
     */
    @Transactional
    @CacheEvict(value = "vectorSearch", allEntries = true)
    public void deleteScamCaseDocument(Long id) {
        deleteDocumentIndex("scamcase", id);
    }

    /**
     * 清空知识库来源的全部索引（保留案例索引）。
     */
    @Transactional
    @CacheEvict(value = "vectorSearch", allEntries = true)
    public void clearKnowledgeIndex() {
        vectorStore.delete(filterBuilder.eq("type", "knowledge").build());
        jdbcTemplate.update("DELETE FROM rag_chunk WHERE source_type = 'knowledge'");
    }

    /**
     * 清空诈骗案例来源的全部索引（保留知识库索引）。
     */
    @Transactional
    @CacheEvict(value = "vectorSearch", allEntries = true)
    public void clearScamCaseIndex() {
        vectorStore.delete(filterBuilder.eq("type", "scamcase").build());
        jdbcTemplate.update("DELETE FROM rag_chunk WHERE source_type = 'scamcase'");
    }

    /**
     * 清空全部索引（向量表与全文表都截断，用于整体重建）。
     */
    @Transactional
    @CacheEvict(value = "vectorSearch", allEntries = true)
    public void clearAllIndexes() {
        jdbcTemplate.update("TRUNCATE vector_store");
        jdbcTemplate.update("TRUNCATE rag_chunk");
    }

    /**
     * 按来源类型删除全部索引。
     *
     * @param type 来源类型（knowledge / scamcase）
     */
    @Transactional
    @CacheEvict(value = "vectorSearch", allEntries = true)
    public void deleteByType(String type) {
        vectorStore.delete(filterBuilder.eq("type", type).build());
        jdbcTemplate.update("DELETE FROM rag_chunk WHERE source_type = ?", type);
    }
}
