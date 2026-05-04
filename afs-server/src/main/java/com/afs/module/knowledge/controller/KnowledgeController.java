package com.afs.module.knowledge.controller;

import com.afs.module.knowledge.entity.Knowledge;
import com.afs.module.knowledge.service.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "知识库管理", description = "知识库的增删改查、文档上传、向量同步等接口")
@RestController
@RequestMapping("/api/knowledge")
@CrossOrigin
public class KnowledgeController {

    @Autowired
    private KnowledgeService knowledgeService;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Operation(summary = "获取知识库列表", description = "支持按分类或关键词筛选")
    @GetMapping
    public Map<String, Object> getKnowledge(
            @Parameter(description = "分类筛选") @RequestParam(required = false) String category,
            @Parameter(description = "关键词搜索") @RequestParam(required = false) String keyword) {
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

    @Operation(summary = "根据 ID 获取知识库")
    @GetMapping("/{id}")
    public Map<String, Object> getKnowledgeById(
            @Parameter(description = "知识库 ID", required = true) @PathVariable Long id) {
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

    @Operation(summary = "添加知识库")
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

    @Operation(summary = "更新知识库")
    @PutMapping("/{id}")
    public Map<String, Object> updateKnowledge(
            @Parameter(description = "知识库 ID", required = true) @PathVariable Long id,
            @RequestBody Knowledge knowledge) {
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

    @Operation(summary = "删除知识库")
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteKnowledge(
            @Parameter(description = "知识库 ID", required = true) @PathVariable Long id) {
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

    @Operation(summary = "语义搜索", description = "基于向量相似度的语义搜索")
    @GetMapping("/search/semantic")
    public Map<String, Object> semanticSearch(
            @Parameter(description = "搜索查询", required = true) @RequestParam String query,
            @Parameter(description = "返回结果数量") @RequestParam(defaultValue = "5") int topK) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> sources = knowledgeService.semanticSearch(query, topK);
            result.put("success", true);
            result.put("results", sources);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @Operation(summary = "同步向量库", description = "将所有知识库和诈骗案例同步到向量库")
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

    @Operation(summary = "上传文档", description = "支持 PDF、Word、Markdown、Excel、TXT 等格式")
    @PostMapping("/upload")
    public Map<String, Object> uploadDocument(
            @Parameter(description = "上传的文件", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "文档分类") @RequestParam(value = "category", required = false) String category,
            @Parameter(description = "文档标题") @RequestParam(value = "title", required = false) String title) {
        return knowledgeService.uploadDocument(file, category, title);
    }

    @Operation(summary = "批量上传文档")
    @PostMapping("/upload/batch")
    public Map<String, Object> uploadDocumentsBatch(
            @Parameter(description = "上传的文件列表", required = true) @RequestParam("files") MultipartFile[] files,
            @Parameter(description = "文档分类") @RequestParam(value = "category", required = false) String category) {
        return knowledgeService.uploadDocumentsBatch(files, category);
    }

    @Operation(summary = "获取支持的文件类型")
    @GetMapping("/supported-types")
    public Map<String, Object> getSupportedTypes() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("types", List.of(
                Map.of("extension", ".pdf", "name", "PDF 文档", "description", "Portable Document Format"),
                Map.of("extension", ".docx", "name", "Word 文档", "description", "Microsoft Word 2007+"),
                Map.of("extension", ".doc", "name", "Word 文档", "description", "Microsoft Word 97-2003"),
                Map.of("extension", ".md", "name", "Markdown 文档", "description", "Markdown 格式"),
                Map.of("extension", ".markdown", "name", "Markdown 文档", "description", "Markdown 格式"),
                Map.of("extension", ".txt", "name", "文本文件", "description", "纯文本格式"),
                Map.of("extension", ".xlsx", "name", "Excel 工作簿", "description", "Microsoft Excel 2007+"),
                Map.of("extension", ".xls", "name", "Excel 工作簿", "description", "Microsoft Excel 97-2003")
        ));
        result.put("maxFileSize", MAX_FILE_SIZE);
        result.put("maxFileSizeMB", MAX_FILE_SIZE / 1024 / 1024);
        return result;
    }
}
