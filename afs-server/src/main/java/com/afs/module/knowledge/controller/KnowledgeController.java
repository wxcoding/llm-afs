package com.afs.module.knowledge.controller;

import com.afs.common.Result;
import com.afs.module.knowledge.dto.AuditSubmitDTO;
import com.afs.module.knowledge.dto.CreateTagDTO;
import com.afs.module.knowledge.dto.KnowledgeUpsertDTO;
import com.afs.module.knowledge.entity.Knowledge;
import com.afs.module.knowledge.entity.KnowledgeAudit;
import com.afs.module.knowledge.entity.KnowledgeTag;
import com.afs.module.knowledge.entity.KnowledgeVersion;
import com.afs.module.knowledge.service.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "知识库管理", description = "知识库的增删改查、文档上传、向量同步、标签管理、版本控制、审核流程等接口")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/knowledge")
@CrossOrigin
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    // ==================== 基础 CRUD ====================

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
    public Map<String, Object> addKnowledge(@RequestBody KnowledgeUpsertDTO request) {
        Map<String, Object> result = new HashMap<>();
        try {
            Knowledge knowledge = new Knowledge();
            knowledge.setTitle(request.getTitle());
            knowledge.setCategory(request.getCategory());
            knowledge.setContent(request.getContent());
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
            @RequestBody KnowledgeUpsertDTO request) {
        Map<String, Object> result = new HashMap<>();
        try {
            Knowledge knowledge = new Knowledge();
            knowledge.setTitle(request.getTitle());
            knowledge.setCategory(request.getCategory());
            knowledge.setContent(request.getContent());
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

    // ==================== 搜索与向量同步 ====================

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

    // ==================== 文档上传 ====================

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

    // ==================== 标签管理 ====================

    @Operation(summary = "创建标签", description = "创建知识库分类标签")
    @PostMapping("/tags")
    public Result<KnowledgeTag> createTag(@RequestBody CreateTagDTO request) {
        return Result.success(knowledgeService.createTag(
                request.getName(), request.getColor(), request.getDescription()));
    }

    @Operation(summary = "获取所有标签", description = "获取系统中所有知识库标签")
    @GetMapping("/tags")
    public Result<List<KnowledgeTag>> getAllTags() {
        return Result.success(knowledgeService.getAllTags());
    }

    @Operation(summary = "删除标签", description = "删除指定标签，同时解除与知识库的关联")
    @DeleteMapping("/tags/{tagId}")
    public Result<Void> deleteTag(
            @Parameter(description = "标签 ID", required = true) @PathVariable Long tagId) {
        knowledgeService.deleteTag(tagId);
        return Result.success();
    }

    @Operation(summary = "为知识库添加标签", description = "建立知识库与标签的关联关系")
    @PostMapping("/tags/knowledge/{knowledgeId}")
    public Result<Void> addTagToKnowledge(
            @Parameter(description = "知识库 ID", required = true) @PathVariable Long knowledgeId,
            @Parameter(description = "标签 ID", required = true) @RequestParam Long tagId) {
        knowledgeService.addTagToKnowledge(knowledgeId, tagId);
        return Result.success();
    }

    @Operation(summary = "移除知识库标签", description = "解除知识库与标签的关联关系")
    @DeleteMapping("/tags/knowledge/{knowledgeId}")
    public Result<Void> removeTagFromKnowledge(
            @Parameter(description = "知识库 ID", required = true) @PathVariable Long knowledgeId,
            @Parameter(description = "标签 ID", required = true) @RequestParam Long tagId) {
        knowledgeService.removeTagFromKnowledge(knowledgeId, tagId);
        return Result.success();
    }

    @Operation(summary = "获取知识库标签", description = "获取指定知识库关联的所有标签")
    @GetMapping("/tags/knowledge/{knowledgeId}")
    public Result<List<KnowledgeTag>> getTagsForKnowledge(
            @Parameter(description = "知识库 ID", required = true) @PathVariable Long knowledgeId) {
        return Result.success(knowledgeService.getTagsForKnowledge(knowledgeId));
    }

    // ==================== 版本控制 ====================

    @Operation(summary = "获取版本历史", description = "获取知识库文档的所有版本记录")
    @GetMapping("/versions/{knowledgeId}")
    public Result<List<KnowledgeVersion>> getVersionHistory(
            @Parameter(description = "知识库 ID", required = true) @PathVariable Long knowledgeId) {
        return Result.success(knowledgeService.getVersionHistory(knowledgeId));
    }

    @Operation(summary = "回滚到指定版本", description = "将知识库文档恢复到指定版本")
    @PostMapping("/versions/{versionId}/rollback")
    public Result<KnowledgeVersion> rollbackToVersion(
            @Parameter(description = "版本 ID", required = true) @PathVariable Long versionId) {
        return Result.success(knowledgeService.rollbackToVersion(versionId));
    }

    // ==================== 审核流程 ====================

    @Operation(summary = "提交审核", description = "提交知识库内容变更申请进行审核")
    @PostMapping("/audit/submit")
    public Result<KnowledgeAudit> submitForAudit(@RequestBody AuditSubmitDTO request) {
        return Result.success(knowledgeService.submitForAudit(
                request.getKnowledgeId(),
                request.getTitle(),
                request.getContent(),
                request.getCategory(),
                request.getSubmitUserId()));
    }

    @Operation(summary = "获取待审核列表", description = "获取所有待审核的知识库变更申请")
    @GetMapping("/audit/pending")
    public Result<List<KnowledgeAudit>> getPendingAudits() {
        return Result.success(knowledgeService.getPendingAudits());
    }

    @Operation(summary = "审核通过", description = "批准知识库变更申请")
    @PostMapping("/audit/{auditId}/approve")
    public Result<KnowledgeAudit> approveAudit(
            @Parameter(description = "审核 ID", required = true) @PathVariable Long auditId,
            @Parameter(description = "审核人 ID", required = true) @RequestParam Long auditUserId,
            @Parameter(description = "审核意见") @RequestParam(required = false) String comment) {
        return Result.success(knowledgeService.approveAudit(auditId, auditUserId, comment));
    }

    @Operation(summary = "审核拒绝", description = "拒绝知识库变更申请")
    @PostMapping("/audit/{auditId}/reject")
    public Result<KnowledgeAudit> rejectAudit(
            @Parameter(description = "审核 ID", required = true) @PathVariable Long auditId,
            @Parameter(description = "审核人 ID", required = true) @RequestParam Long auditUserId,
            @Parameter(description = "拒绝理由") @RequestParam(required = false) String comment) {
        return Result.success(knowledgeService.rejectAudit(auditId, auditUserId, comment));
    }
}
