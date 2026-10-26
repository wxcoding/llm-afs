package com.afs.module.rag.ingestion;

import com.afs.module.rag.search.FullTextTokenizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * RagIngestionService 单元测试。
 * 覆盖知识库/诈骗案例文档的向量化入库（先删后写、确定性 chunk ID、全文索引批量写入）、
 * 按文档/按类型删除索引、清空索引与全量重建时的表清理，以及向量写入失败、
 * 全文索引写入失败两类部分失败分支。
 */
class RagIngestionServiceTest {

    private VectorStore vectorStore;
    private JdbcTemplate jdbcTemplate;
    private RagIngestionService service;

    @BeforeEach
    void setUp() {
        // 向量库与全文索引均使用 mock，避免真实 Embedding 与数据库调用
        vectorStore = mock(VectorStore.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        service = new RagIngestionService(vectorStore, jdbcTemplate);
    }

    // 知识库文档入库：先按 dbId+type 删除旧索引，再写入向量与全文索引
    @Test
    void addKnowledgeDocumentDeletesOldIndexThenWritesChunks() throws Exception {
        service.addKnowledgeDocument(1L, "防骗指南", "prevention", "警惕冒充客服诈骗");

        // 向量侧必须先删后写，保证重建时无残留
        InOrder order = inOrder(vectorStore);
        order.verify(vectorStore).delete(any(Filter.Expression.class));
        order.verify(vectorStore).add(anyList());

        ArgumentCaptor<List<Document>> chunksCaptor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(chunksCaptor.capture());
        List<Document> chunks = chunksCaptor.getValue();
        assertEquals(1, chunks.size());

        // 短文本只产生一个 chunk，ID 由 sourceType:sourceId:序号 确定性生成
        String expectedChunkId = UUID.nameUUIDFromBytes(
                "knowledge:1:0".getBytes(StandardCharsets.UTF_8)).toString();
        Document chunk = chunks.get(0);
        assertEquals(expectedChunkId, chunk.getId());
        assertEquals("防骗指南", chunk.getMetadata().get("title"));
        assertEquals("prevention", chunk.getMetadata().get("category"));
        assertEquals("1", chunk.getMetadata().get("dbId"));
        assertEquals("knowledge", chunk.getMetadata().get("type"));
        assertEquals("知识库", chunk.getMetadata().get("source"));
        assertTrue(chunk.getText().contains("冒充客服"));

        // 全文索引：先删 rag_chunk 旧行，再批量插入新行
        verify(jdbcTemplate).update(contains("DELETE FROM rag_chunk"), eq("knowledge"), eq(1L));

        ArgumentCaptor<BatchPreparedStatementSetter> setterCaptor =
                ArgumentCaptor.forClass(BatchPreparedStatementSetter.class);
        verify(jdbcTemplate).batchUpdate(contains("INSERT INTO rag_chunk"), setterCaptor.capture());
        BatchPreparedStatementSetter setter = setterCaptor.getValue();
        assertEquals(1, setter.getBatchSize());

        // 校验批量参数绑定与 search_text 分词结果
        PreparedStatement ps = mock(PreparedStatement.class);
        setter.setValues(ps, 0);
        verify(ps).setString(1, expectedChunkId);
        verify(ps).setString(2, "knowledge");
        verify(ps).setLong(3, 1L);
        verify(ps).setString(4, "防骗指南");
        verify(ps).setString(5, "prevention");
        verify(ps).setString(6, chunk.getText());
        String expectedSearchText = String.join(" ",
                FullTextTokenizer.tokenize("防骗指南 prevention " + chunk.getText()));
        verify(ps).setString(7, expectedSearchText);
    }

    // 诈骗案例文档入库：正文由案例经过与防范提示拼接，元数据带 caseType
    @Test
    void addScamCaseDocumentConcatenatesContentAndTips() {
        service.addScamCaseDocument(2L, "刷单被骗", "刷单诈骗", "先给小额返利", "不要刷单");

        ArgumentCaptor<List<Document>> chunksCaptor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(chunksCaptor.capture());
        List<Document> chunks = chunksCaptor.getValue();
        assertEquals(1, chunks.size());

        String expectedChunkId = UUID.nameUUIDFromBytes(
                "scamcase:2:0".getBytes(StandardCharsets.UTF_8)).toString();
        Document chunk = chunks.get(0);
        assertEquals(expectedChunkId, chunk.getId());
        assertEquals("诈骗案例", chunk.getMetadata().get("source"));
        assertEquals("scamcase", chunk.getMetadata().get("type"));
        assertEquals("刷单诈骗", chunk.getMetadata().get("caseType"));
        assertEquals("刷单诈骗", chunk.getMetadata().get("category"));
        assertEquals("2", chunk.getMetadata().get("dbId"));
        assertTrue(chunk.getText().contains("案例经过：先给小额返利"));
        assertTrue(chunk.getText().contains("防范提示：不要刷单"));

        verify(jdbcTemplate).update(contains("DELETE FROM rag_chunk"), eq("scamcase"), eq(2L));
        verify(jdbcTemplate).batchUpdate(contains("INSERT INTO rag_chunk"),
                any(BatchPreparedStatementSetter.class));
    }

    // 长文档批量入库：切分为多个 chunk，ID 按序号递增且与批量写入条数一致
    @Test
    void longDocumentIsSplitIntoMultipleChunksWithDeterministicIds() {
        service.addKnowledgeDocument(7L, "长文", "general", longVariedContent());

        ArgumentCaptor<List<Document>> chunksCaptor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(chunksCaptor.capture());
        List<Document> chunks = chunksCaptor.getValue();
        assertTrue(chunks.size() >= 2, "长文本应被切分为多个 chunk");

        for (int i = 0; i < chunks.size(); i++) {
            String expectedId = UUID.nameUUIDFromBytes(
                    ("knowledge:7:" + i).getBytes(StandardCharsets.UTF_8)).toString();
            assertEquals(expectedId, chunks.get(i).getId());
        }

        ArgumentCaptor<BatchPreparedStatementSetter> setterCaptor =
                ArgumentCaptor.forClass(BatchPreparedStatementSetter.class);
        verify(jdbcTemplate).batchUpdate(contains("INSERT INTO rag_chunk"), setterCaptor.capture());
        assertEquals(chunks.size(), setterCaptor.getValue().getBatchSize());
    }

    // 向量写入失败：异常直接抛出，且不再写全文索引（由事务回滚保证一致性）
    // 注：内容需超过 5 字符，否则 TokenTextSplitter 过滤短块导致方法提前返回、触及不到 mock
    @Test
    void vectorStoreFailurePropagatesAndSkipsFullTextIndexWrite() {
        doThrow(new IllegalStateException("向量写入失败")).when(vectorStore).add(anyList());

        assertThrows(IllegalStateException.class,
                () -> service.addKnowledgeDocument(1L, "防骗指南", "prevention", "电信诈骗防范指南正文内容"));

        verify(jdbcTemplate, never()).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
    }

    // 全文索引写入失败：向量已写入后异常抛出（部分失败分支）
    @Test
    void fullTextIndexFailurePropagatesAfterVectorWrite() {
        doThrow(new IllegalStateException("索引写入失败")).when(jdbcTemplate)
                .batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));

        assertThrows(IllegalStateException.class,
                () -> service.addKnowledgeDocument(1L, "防骗指南", "prevention", "电信诈骗防范指南正文内容"));

        verify(vectorStore).add(anyList());
    }

    // 删除知识库文档：向量侧按 dbId+type 双条件删除，同时清理 rag_chunk 行
    @Test
    void deleteKnowledgeDocumentRemovesVectorAndChunkIndex() {
        service.deleteKnowledgeDocument(1L);

        ArgumentCaptor<Filter.Expression> filterCaptor = ArgumentCaptor.forClass(Filter.Expression.class);
        verify(vectorStore).delete(filterCaptor.capture());
        Filter.Expression expression = filterCaptor.getValue();
        assertEquals(Filter.ExpressionType.AND, expression.type());
        assertEqExpression((Filter.Expression) expression.left(), "dbId", "1");
        assertEqExpression((Filter.Expression) expression.right(), "type", "knowledge");

        verify(jdbcTemplate).update(contains("DELETE FROM rag_chunk"), eq("knowledge"), eq(1L));
        verify(vectorStore, never()).add(anyList());
    }

    // 删除诈骗案例文档：使用 scamcase 来源类型
    @Test
    void deleteScamCaseDocumentUsesScamcaseSourceType() {
        service.deleteScamCaseDocument(3L);

        verify(vectorStore).delete(any(Filter.Expression.class));
        verify(jdbcTemplate).update(contains("DELETE FROM rag_chunk"), eq("scamcase"), eq(3L));
    }

    // 清空知识库索引：向量侧按 type=knowledge 单条件删除
    @Test
    void clearKnowledgeIndexDeletesByKnowledgeType() {
        service.clearKnowledgeIndex();

        ArgumentCaptor<Filter.Expression> filterCaptor = ArgumentCaptor.forClass(Filter.Expression.class);
        verify(vectorStore).delete(filterCaptor.capture());
        assertEqExpression(filterCaptor.getValue(), "type", "knowledge");
        verify(jdbcTemplate).update("DELETE FROM rag_chunk WHERE source_type = 'knowledge'");
    }

    // 清空诈骗案例索引：向量侧按 type=scamcase 单条件删除
    @Test
    void clearScamCaseIndexDeletesByScamcaseType() {
        service.clearScamCaseIndex();

        ArgumentCaptor<Filter.Expression> filterCaptor = ArgumentCaptor.forClass(Filter.Expression.class);
        verify(vectorStore).delete(filterCaptor.capture());
        assertEqExpression(filterCaptor.getValue(), "type", "scamcase");
        verify(jdbcTemplate).update("DELETE FROM rag_chunk WHERE source_type = 'scamcase'");
    }

    // 生成足够长且不可压缩的中文文本，确保超过 TokenTextSplitter 的默认块大小
    private static String longVariedContent() {
        String chars = "的一是不了在人有我他这中大来上国个到说们为子和你地出道也时年得就那要下以生会自着去之过家学对";
        Random random = new Random(42);
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 3000; i++) {
            content.append(chars.charAt(random.nextInt(chars.length())));
        }
        return content.toString();
    }

    private static void assertEqExpression(Filter.Expression expression, String key, Object value) {
        assertEquals(Filter.ExpressionType.EQ, expression.type());
        assertEquals(key, ((Filter.Key) expression.left()).key());
        assertEquals(value, ((Filter.Value) expression.right()).value());
    }
}
