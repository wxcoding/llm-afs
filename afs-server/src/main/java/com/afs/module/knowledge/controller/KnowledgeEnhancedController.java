package com.afs.module.knowledge.controller;

import com.afs.common.Result;
import com.afs.module.knowledge.entity.KnowledgeTag;
import com.afs.module.knowledge.entity.KnowledgeVersion;
import com.afs.module.knowledge.entity.KnowledgeAudit;
import com.afs.module.knowledge.service.KnowledgeEnhancedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "知识库增强功能", description = "知识库标签管理、版本控制、审核流程等接口")
@RestController
@RequestMapping("/api/knowledge/enhanced")
@CrossOrigin
public class KnowledgeEnhancedController {

    @Autowired
    private KnowledgeEnhancedService enhancedService;

    @Operation(summary = "创建标签", description = "创建知识库分类标签")
    @PostMapping("/tags")
    public Result<KnowledgeTag> createTag(@RequestBody Map<String, String> params) {
        return Result.success(enhancedService.createTag(
                params.get("name"), params.get("color"), params.get("description")));
    }

    @Operation(summary = "获取所有标签", description = "获取系统中所有知识库标签")
    @GetMapping("/tags")
    public Result<List<KnowledgeTag>> getAllTags() {
        return Result.success(enhancedService.getAllTags());
    }

    @Operation(summary = "删除标签", description = "删除指定标签，同时解除与知识库的关联")
    @DeleteMapping("/tags/{tagId}")
    public Result<Void> deleteTag(
            @Parameter(description = "标签 ID", required = true) @PathVariable Long tagId) {
        enhancedService.deleteTag(tagId);
        return Result.success();
    }

    @Operation(summary = "为知识库添加标签", description = "建立知识库与标签的关联关系")
    @PostMapping("/tags/knowledge/{knowledgeId}")
    public Result<Void> addTagToKnowledge(
            @Parameter(description = "知识库 ID", required = true) @PathVariable Long knowledgeId,
            @Parameter(description = "标签 ID", required = true) @RequestParam Long tagId) {
        enhancedService.addTagToKnowledge(knowledgeId, tagId);
        return Result.success();
    }

    @Operation(summary = "移除知识库标签", description = "解除知识库与标签的关联关系")
    @DeleteMapping("/tags/knowledge/{knowledgeId}")
    public Result<Void> removeTagFromKnowledge(
            @Parameter(description = "知识库 ID", required = true) @PathVariable Long knowledgeId,
            @Parameter(description = "标签 ID", required = true) @RequestParam Long tagId) {
        enhancedService.removeTagFromKnowledge(knowledgeId, tagId);
        return Result.success();
    }

    @Operation(summary = "获取知识库标签", description = "获取指定知识库关联的所有标签")
    @GetMapping("/tags/knowledge/{knowledgeId}")
    public Result<List<KnowledgeTag>> getTagsForKnowledge(
            @Parameter(description = "知识库 ID", required = true) @PathVariable Long knowledgeId) {
        return Result.success(enhancedService.getTagsForKnowledge(knowledgeId));
    }

    @Operation(summary = "获取版本历史", description = "获取知识库文档的所有版本记录")
    @GetMapping("/versions/{knowledgeId}")
    public Result<List<KnowledgeVersion>> getVersionHistory(
            @Parameter(description = "知识库 ID", required = true) @PathVariable Long knowledgeId) {
        return Result.success(enhancedService.getVersionHistory(knowledgeId));
    }

    @Operation(summary = "回滚到指定版本", description = "将知识库文档恢复到指定版本")
    @PostMapping("/versions/{versionId}/rollback")
    public Result<KnowledgeVersion> rollbackToVersion(
            @Parameter(description = "版本 ID", required = true) @PathVariable Long versionId) {
        return Result.success(enhancedService.rollbackToVersion(versionId));
    }

    @Operation(summary = "提交审核", description = "提交知识库内容变更申请进行审核")
    @PostMapping("/audit/submit")
    public Result<KnowledgeAudit> submitForAudit(@RequestBody Map<String, Object> params) {
        Object knowledgeIdObj = params.get("knowledgeId");
        Long knowledgeId = knowledgeIdObj != null ? Long.valueOf(knowledgeIdObj.toString()) : null;
        return Result.success(enhancedService.submitForAudit(
                knowledgeId,
                (String) params.get("title"),
                (String) params.get("content"),
                (String) params.get("category"),
                Long.valueOf(params.get("submitUserId").toString())));
    }

    @Operation(summary = "获取待审核列表", description = "获取所有待审核的知识库变更申请")
    @GetMapping("/audit/pending")
    public Result<List<KnowledgeAudit>> getPendingAudits() {
        return Result.success(enhancedService.getPendingAudits());
    }

    @Operation(summary = "审核通过", description = "批准知识库变更申请")
    @PostMapping("/audit/{auditId}/approve")
    public Result<KnowledgeAudit> approveAudit(
            @Parameter(description = "审核 ID", required = true) @PathVariable Long auditId,
            @Parameter(description = "审核人 ID", required = true) @RequestParam Long auditUserId,
            @Parameter(description = "审核意见") @RequestParam(required = false) String comment) {
        return Result.success(enhancedService.approveAudit(auditId, auditUserId, comment));
    }

    @Operation(summary = "审核拒绝", description = "拒绝知识库变更申请")
    @PostMapping("/audit/{auditId}/reject")
    public Result<KnowledgeAudit> rejectAudit(
            @Parameter(description = "审核 ID", required = true) @PathVariable Long auditId,
            @Parameter(description = "审核人 ID", required = true) @RequestParam Long auditUserId,
            @Parameter(description = "拒绝理由") @RequestParam(required = false) String comment) {
        return Result.success(enhancedService.rejectAudit(auditId, auditUserId, comment));
    }
}
