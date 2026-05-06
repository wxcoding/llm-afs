package com.afs.module.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * RAG服务类
 * 
 * 负责向量数据库的管理，包括文档向量化、相似度搜索、上下文构建等功能
 */
@Service
public class RagService {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 向量相似度搜索
     * 
     * @param query 搜索查询词
     * @param topK 返回结果数量
     * @return 相关文档列表
     */
    public List<Document> searchRelevant(String query, int topK) {
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .similarityThreshold(0.5)
                        .build()
        );
    }

    /**
     * 构建上下文文本
     * 
     * @param query 搜索查询词
     * @param topK 返回结果数量
     * @return 格式化的上下文文本
     */
    public String buildContext(String query, int topK) {
        List<Document> documents = searchRelevant(query, topK);
        
        if (documents.isEmpty()) {
            return "";
        }
        
        // 将检索到的文档格式化为上下文文本
        return documents.stream()
                .map(doc -> {
                    String source = (String) doc.getMetadata().getOrDefault("source", "未知来源");
                    String title = (String) doc.getMetadata().getOrDefault("title", "");
                    return "【" + source + (title.isEmpty() ? "" : " - " + title) + "】\n" + doc.getText();
                })
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * 带元数据的向量搜索
     * 
     * @param query 搜索查询词
     * @param topK 返回结果数量
     * @return 包含元数据的搜索结果列表
     */
    public List<Map<String, Object>> searchWithMetadata(String query, int topK) {
        List<Document> documents = searchRelevant(query, topK);
        
        // 将Document转换为包含元数据的Map
        return documents.stream()
                .map(doc -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("content", doc.getText());
                    map.put("source", doc.getMetadata().getOrDefault("source", "未知来源"));
                    map.put("title", doc.getMetadata().getOrDefault("title", ""));
                    map.put("id", doc.getMetadata().getOrDefault("dbId", ""));
                    map.put("type", doc.getMetadata().getOrDefault("type", "knowledge"));
                    map.put("score", doc.getMetadata().getOrDefault("distance", 0.0));
                    return map;
                })
                .collect(Collectors.toList());
    }

    public void addKnowledgeDocument(Long id, String title, String category, String content) {
        String uuid = UUID.randomUUID().toString();
        Document doc = Document.builder()
                .text(content)
                .id(uuid)
                .metadata("source", "知识库")
                .metadata("title", title)
                .metadata("category", category)
                .metadata("dbId", id.toString())
                .metadata("type", "knowledge")
                .build();

        vectorStore.add(List.of(doc));
    }

    public void addScamCaseDocument(Long id, String title, String type, String content, String tips) {
        String fullContent = "案例经过：" + content + "\n防范提示：" + tips;
        String uuid = UUID.randomUUID().toString();

        Document doc = Document.builder()
                .text(fullContent)
                .id(uuid)
                .metadata("source", "诈骗案例")
                .metadata("title", title)
                .metadata("caseType", type)
                .metadata("dbId", id.toString())
                .metadata("type", "scamcase")
                .build();

        vectorStore.add(List.of(doc));
    }

    public void deleteKnowledgeDocument(Long id) {
        jdbcTemplate.update("DELETE FROM vector_store WHERE metadata ->> 'dbId' = ? AND metadata ->> 'type' = 'knowledge'",
                id.toString());
    }

    public void deleteScamCaseDocument(Long id) {
        jdbcTemplate.update("DELETE FROM vector_store WHERE metadata ->> 'dbId' = ? AND metadata ->> 'type' = 'scamcase'",
                id.toString());
    }

    public void clearKnowledgeVector() {
        jdbcTemplate.update("DELETE FROM vector_store WHERE metadata ->> 'type' = 'knowledge'");
    }

    public void clearScamCaseVector() {
        jdbcTemplate.update("DELETE FROM vector_store WHERE metadata ->> 'type' = 'scamcase'");
    }

    public void clearAllVector() {
        jdbcTemplate.update("TRUNCATE vector_store");
    }
}
