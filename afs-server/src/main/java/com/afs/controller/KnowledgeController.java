package com.afs.controller;

import com.afs.entity.Knowledge;
import com.afs.service.KnowledgeService;
import com.afs.service.RagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库控制器
 *
 * 提供知识库的 CRUD 接口，支持按分类查询、关键词搜索和语义检索。
 */
@RestController
@RequestMapping("/api/knowledge")
@CrossOrigin
public class KnowledgeController {

    @Autowired
    private KnowledgeService knowledgeService;

    @Autowired
    private RagService ragService;

    /**
     * 获取知识列表
     *
     * 支持按分类筛选或关键词搜索。
     *
     * @param category 知识分类（可选）
     * @param keyword  搜索关键词（可选）
     * @return 知识列表
     */
    @GetMapping
    public Map<String, Object> getKnowledge(@RequestParam(required = false) String category,
                                             @RequestParam(required = false) String keyword) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Knowledge> list;
            if (keyword != null && !keyword.isEmpty()) {
                list = knowledgeService.searchKnowledge(keyword);
            } else if (category != null && !category.isEmpty()) {
                list = knowledgeService.getKnowledgeByCategory(category);
            } else {
                list = knowledgeService.getAllKnowledge();
            }
            result.put("success", true);
            result.put("list", list);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取知识详情
     *
     * @param id 知识 ID
     * @return 知识详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> getKnowledgeById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Knowledge knowledge = knowledgeService.getKnowledgeById(id);
            result.put("success", true);
            result.put("knowledge", knowledge);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 添加知识条目
     *
     * @param knowledge 知识对象
     * @return 创建结果
     */
    @PostMapping
    public Map<String, Object> addKnowledge(@RequestBody Knowledge knowledge) {
        Map<String, Object> result = new HashMap<>();
        try {
            Knowledge created = knowledgeService.addKnowledge(knowledge);
            result.put("success", true);
            result.put("message", "添加成功");
            result.put("knowledge", created);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 更新知识条目
     *
     * @param id       知识 ID
     * @param knowledge 包含更新内容的知识对象
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public Map<String, Object> updateKnowledge(@PathVariable Long id, @RequestBody Knowledge knowledge) {
        Map<String, Object> result = new HashMap<>();
        try {
            Knowledge updated = knowledgeService.updateKnowledge(id, knowledge);
            result.put("success", true);
            result.put("message", "更新成功");
            result.put("knowledge", updated);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 删除知识条目
     *
     * @param id 知识 ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteKnowledge(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            knowledgeService.deleteKnowledge(id);
            result.put("success", true);
            result.put("message", "删除成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 语义检索知识
     *
     * 基于向量相似度进行语义检索，返回最相关的知识条目。
     *
     * @param query 检索查询
     * @param topK  返回结果数量（默认5）
     * @return 检索结果列表
     */
    @GetMapping("/search/semantic")
    public Map<String, Object> semanticSearch(@RequestParam String query,
                                               @RequestParam(defaultValue = "5") int topK) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> sources = ragService.searchWithMetadata(query, topK);
            result.put("success", true);
            result.put("results", sources);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 同步所有知识到向量库
     *
     * 用于修复或重建向量库数据。
     *
     * @return 同步结果
     */
    @PostMapping("/sync-vector")
    public Map<String, Object> syncVectorStore() {
        Map<String, Object> result = new HashMap<>();
        try {
            knowledgeService.syncAllToVectorStore();
            result.put("success", true);
            result.put("message", "同步完成");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
