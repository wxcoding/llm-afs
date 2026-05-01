package com.afs.service;

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
 * RAG（检索增强生成）服务
 *
 * 负责向量库的检索和文档管理，将知识库和诈骗案例向量化存储，
 * 支持基于语义相似度的知识检索，为 AI 对话提供上下文支持。
 */
@Service
public class RagService {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 检索相关文档
     *
     * @param query 检索查询文本
     * @param topK  返回的最相似文档数量
     * @return 相似度最高的文档列表
     */
    public List<Document> searchRelevant(String query, int topK) {
        // 构建搜索请求，设置查询文本、返回数量和相似度阈值
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)       // 查询文本
                        .topK(topK)         // 返回的最相似文档数量
                        .similarityThreshold(0.5)  // 相似度阈值，低于此值的结果被过滤
                        .build()
        );
    }

    /**
     * 构建检索上下文
     *
     * 将检索到的多个文档内容拼接成一段文本，作为 AI 回答的参考上下文。
     *
     * @param query 检索查询文本
     * @param topK  返回的最相似文档数量
     * @return 拼接后的上下文文本，如果无结果则返回空字符串
     */
    public String buildContext(String query, int topK) {
        // 检索相关文档
        List<Document> documents = searchRelevant(query, topK);
        
        // 如果没有检索到文档，返回空字符串
        if (documents.isEmpty()) {
            return "";
        }
        
        // 将检索到的文档拼接成上下文文本
        return documents.stream()
                .map(doc -> {
                    // 获取文档来源和标题
                    String source = (String) doc.getMetadata().getOrDefault("source", "未知来源");
                    String title = (String) doc.getMetadata().getOrDefault("title", "");
                    // 格式化文档内容，添加来源和标题信息
                    return "【" + source + (title.isEmpty() ? "" : " - " + title) + "】\n" + doc.getText();
                })
                .collect(Collectors.joining("\n\n")); // 文档之间用空行分隔
    }

    /**
     * 带元数据的语义检索
     *
     * 返回检索结果及其来源信息，用于前端展示参考资料列表。
     *
     * @param query 检索查询文本
     * @param topK  返回的最相似文档数量
     * @return 包含内容、来源、标题、ID、相似度分数的检索结果列表
     */
    public List<Map<String, Object>> searchWithMetadata(String query, int topK) {
        // 检索相关文档
        List<Document> documents = searchRelevant(query, topK);
        
        // 将文档转换为前端需要的格式
        return documents.stream()
                .map(doc -> {
                    Map<String, Object> map = new HashMap<>();
                    // 提取文档内容和元数据
                    map.put("content", doc.getText());           // 文档内容
                    map.put("source", doc.getMetadata().getOrDefault("source", "未知来源")); // 来源
                    map.put("title", doc.getMetadata().getOrDefault("title", "")); // 标题
                    map.put("id", doc.getMetadata().getOrDefault("dbId", "")); // 数据库ID
                    map.put("score", doc.getMetadata().getOrDefault("distance", 0.0)); // 相似度分数
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * 添加知识库文档到向量库
     *
     * @param id       知识库条目 ID
     * @param title    知识标题
     * @param category 知识分类
     * @param content  知识正文内容
     */
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

    /**
     * 添加诈骗案例文档到向量库
     *
     * @param id      案例 ID
     * @param title   案例标题
     * @param type    案例类型
     * @param content 案例描述
     * @param tips    防范提示
     */
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

    /**
     * 从向量库删除知识库文档
     *
     * @param id 知识库条目 ID
     */
    public void deleteKnowledgeDocument(Long id) {
        jdbcTemplate.update("DELETE FROM vector_store WHERE metadata ->> 'dbId' = ? AND metadata ->> 'type' = 'knowledge'",
                id.toString());
    }

    /**
     * 从向量库删除诈骗案例文档
     *
     * @param id 案例 ID
     */
    public void deleteScamCaseDocument(Long id) {
        jdbcTemplate.update("DELETE FROM vector_store WHERE metadata ->> 'dbId' = ? AND metadata ->> 'type' = 'scamcase'",
                id.toString());
    }

    /**
     * 清空知识库向量
     */
    public void clearKnowledgeVector() {
        jdbcTemplate.update("DELETE FROM vector_store WHERE metadata ->> 'type' = 'knowledge'");
    }

    /**
     * 清空诈骗案例向量
     */
    public void clearScamCaseVector() {
        jdbcTemplate.update("DELETE FROM vector_store WHERE metadata ->> 'type' = 'scamcase'");
    }

    /**
     * 清空所有向量
     */
    public void clearAllVector() {
        jdbcTemplate.update("TRUNCATE vector_store");
    }
}
