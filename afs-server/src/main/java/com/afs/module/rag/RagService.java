package com.afs.module.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RAG服务类
 * 
 * 负责向量数据库的管理，包括文档向量化、相似度搜索、上下文构建等功能。
 * 
 * 升级说明（Spring AI 1.1.7）：
 * - 使用 TokenTextSplitter 实现智能文档切分
 * - 使用 FilterExpression 实现元数据过滤
 * - 移除了直接 SQL 操作，改用统一的 VectorStore API
 * - 添加了异常处理和缓存机制
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 过滤表达式构建器（DSL，类型安全）
     */
    private final FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();

    /**
     * TokenTextSplitter - Spring AI 文本分块器
     * 
     * 使用默认配置：
     * - defaultChunkSize: 800 tokens（每段目标大小）
     * - minChunkSizeChars: 350 chars（最小字符数）
     * - minChunkLengthToEmbed: 5 chars（最小嵌入长度）
     * - maxNumChunks: 10000（最大段数）
     * - keepSeparator: true（保留分隔符）
     */
    private final TokenTextSplitter splitter = new TokenTextSplitter();

    /**
     * 向量相似度搜索（增强版）
     * 
     * 特性：
     * 1. 添加查询结果缓存，减少重复API调用
     * 2. 完善的异常处理和日志记录
     * 3. 支持降级策略
     * 
     * @param query 搜索查询词
     * @param topK 返回结果数量
     * @return 相关文档列表
     */
    @Cacheable(value = "vectorSearch", key = "#query + '_' + #topK", unless = "#result == null || #result.isEmpty()")
    public List<Document> searchRelevant(String query, int topK) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("开始向量搜索，查询词: {}, topK: {}", query, topK);
            
            List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(0.5)
                    .build()
            );
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("向量搜索成功，查询词: {}, 结果数: {}, 耗时: {}ms", 
                     query, results.size(), duration);
            
            return results;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("向量搜索失败，查询词: {}, topK: {}, 耗时: {}ms, 错误: {}", 
                     query, topK, duration, e.getMessage(), e);
            
            // 根据错误类型决定是否需要降级
            if (isNetworkError(e)) {
                log.warn("检测到网络错误，触发降级策略");
                return handleNetworkFailure(query, topK);
            }
            
            throw new RuntimeException("向量搜索失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 判断是否为网络相关错误
     */
    private boolean isNetworkError(Exception e) {
        String message = e.getMessage().toLowerCase();
        return message.contains("connection reset") ||
               message.contains("timeout") ||
               message.contains("connect timed out") ||
               message.contains("read timed out") ||
               message.contains("network") ||
               message.contains("i/o error");
    }
    
    /**
     * 网络故障时的降级策略
     * 
     * 做法：
     * 1. 返回空结果或缓存的历史结果
     * 2. 记录降级事件用于后续分析
     * 3. 触发告警通知运维团队
     */
    private List<Document> handleNetworkFailure(String query, int topK) {
        log.warn("使用降级策略：返回空结果列表。建议：1)检查网络连接 2)检查API密钥 3)查看DashScope服务状态");
        
        // 可选：这里可以返回最近缓存的相似查询结果
        // 或者返回一个特殊的标记文档，告诉上层服务使用了降级
        
        return List.of(); // 返回空列表，让上层服务知道没有检索到相关内容
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
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            String source = String.valueOf(doc.getMetadata().getOrDefault("source", "未知来源"));
            String title = String.valueOf(doc.getMetadata().getOrDefault("title", ""));
            
            if (i > 0) {
                context.append("\n\n");
            }
            context.append("【").append(source);
            if (!title.isEmpty() && !"null".equals(title)) {
                context.append(" - ").append(title);
            }
            context.append("】\n").append(doc.getText());
        }
        return context.toString();
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
                map.put("source", String.valueOf(doc.getMetadata().getOrDefault("source", "未知来源")));
                map.put("title", String.valueOf(doc.getMetadata().getOrDefault("title", "")));
                map.put("id", String.valueOf(doc.getMetadata().getOrDefault("dbId", "")));
                map.put("type", String.valueOf(doc.getMetadata().getOrDefault("type", "knowledge")));
                map.put("score", doc.getMetadata().getOrDefault("distance", 0.0));
                return map;
            })
            .collect(Collectors.toList());
    }

    /**
     * 添加知识文档（使用 TokenTextSplitter 智能切分）
     * 
     * Spring AI 1.1.7 新增功能：
     * - 使用 TokenTextSplitter 将长文档智能切分为多个 chunk
     * - 每个 chunk 保留完整的元数据
     * - 提高检索精度和 LLM 上下文利用效率
     * 
     * @param id 知识ID
     * @param title 知识标题
     * @param category 知识分类
     * @param content 知识内容
     */
    public void addKnowledgeDocument(Long id, String title, String category, String content) {
        // 构建基础文档
        Document doc = Document.builder()
            .text(content)
            .metadata("source", "知识库")
            .metadata("title", title)
            .metadata("category", category)
            .metadata("dbId", id.toString())
            .metadata("type", "knowledge")
            .build();

        // 使用 TokenTextSplitter 切分文档（元数据会自动继承）
        List<Document> chunks = splitter.apply(List.of(doc));

        // 添加到向量库
        vectorStore.add(chunks);
    }

    /**
     * 添加诈骗案例文档（使用 TokenTextSplitter 智能切分）
     * 
     * @param id 案例ID
     * @param title 案例标题
     * @param type 案例类型
     * @param content 案例经过
     * @param tips 防范提示
     */
    public void addScamCaseDocument(Long id, String title, String type, String content, String tips) {
        // 构建完整内容
        String fullContent = "案例经过：" + content + "\n防范提示：" + tips;

        // 构建基础文档
        Document doc = Document.builder()
            .text(fullContent)
            .metadata("source", "诈骗案例")
            .metadata("title", title)
            .metadata("caseType", type)
            .metadata("dbId", id.toString())
            .metadata("type", "scamcase")
            .build();

        // 使用 TokenTextSplitter 切分文档（元数据会自动继承）
        List<Document> chunks = splitter.apply(List.of(doc));

        // 添加到向量库
        vectorStore.add(chunks);
    }

    /**
     * 删除知识文档（使用 Filter.Expression）
     * 
     * Spring AI 1.1.x 使用 FilterExpressionBuilder 构建过滤表达式
     * 
     * @param id 知识ID
     */
    public void deleteKnowledgeDocument(Long id) {
        // 构建过滤表达式：dbId = ? AND type = 'knowledge'
        var filter = filterBuilder.and(
            filterBuilder.eq("dbId", id.toString()),
            filterBuilder.eq("type", "knowledge")
        ).build();

        // 删除匹配文档
        vectorStore.delete(filter);
    }

    /**
     * 删除诈骗案例文档（使用 Filter.Expression）
     * 
     * @param id 案例ID
     */
    public void deleteScamCaseDocument(Long id) {
        // 构建过滤表达式：dbId = ? AND type = 'scamcase'
        var filter = filterBuilder.and(
            filterBuilder.eq("dbId", id.toString()),
            filterBuilder.eq("type", "scamcase")
        ).build();

        // 删除匹配文档
        vectorStore.delete(filter);
    }

    /**
     * 清空知识库向量（使用 Filter.Expression）
     */
    public void clearKnowledgeVector() {
        // 构建过滤表达式：type = 'knowledge'
        var filter = filterBuilder.eq("type", "knowledge").build();

        vectorStore.delete(filter);
    }

    /**
     * 清空诈骗案例向量（使用 Filter.Expression）
     */
    public void clearScamCaseVector() {
        // 构建过滤表达式：type = 'scamcase'
        var filter = filterBuilder.eq("type", "scamcase").build();

        vectorStore.delete(filter);
    }

    /**
     * 清空所有向量（仍需使用 JdbcTemplate，因为 FilterExpression 不支持）
     */
    public void clearAllVector() {
        // TRUNCATE 操作无法通过 FilterExpression 实现，继续使用 JdbcTemplate
        jdbcTemplate.update("TRUNCATE vector_store");
    }

    // ========== 高级查询功能（Filter.Expression 增强示例）==========

    /**
     * 根据分类检索知识（Filter.Expression 多条件查询示例）
     * 
     * @param query 搜索查询词
     * @param category 分类名称
     * @param topK 返回结果数量
     * @return 相关文档列表
     */
    public List<Document> searchByCategory(String query, String category, int topK) {
        // 构建过滤表达式：type = 'knowledge' AND category = ?
        var filter = filterBuilder.and(
            filterBuilder.eq("type", "knowledge"),
            filterBuilder.eq("category", category)
        ).build();

        return vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(topK)
                .filterExpression(filter)
                .build()
        );
    }

    /**
     * 批量删除指定类型的文档（Filter.Expression 批量操作示例）
     * 
     * @param type 文档类型（knowledge 或 scamcase）
     */
    public void deleteByType(String type) {
        var filter = filterBuilder.eq("type", type).build();

        vectorStore.delete(filter);
    }
}
