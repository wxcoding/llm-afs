package com.afs.module.conversation.controller;

import com.afs.common.Result;
import com.afs.module.conversation.entity.ConversationFavorite;
import com.afs.module.conversation.entity.ConversationTemplate;
import com.afs.module.conversation.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "会话管理", description = "对话收藏、会话模板管理等接口")
@RestController
@RequestMapping("/api/conversation")
@CrossOrigin
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    @Operation(summary = "添加收藏", description = "将对话消息添加到收藏列表")
    @PostMapping("/favorites")
    public Result<ConversationFavorite> addFavorite(@RequestBody Map<String, Object> params) {
        Object userIdObj = params.get("userId");
        Object messageIdObj = params.get("messageId");

        if (userIdObj == null || messageIdObj == null) {
            return Result.error("用户ID和消息ID不能为空");
        }

        return Result.success(conversationService.addFavorite(
                Long.valueOf(userIdObj.toString()),
                Long.valueOf(messageIdObj.toString()),
                (String) params.get("title")));
    }

    @Operation(summary = "取消收藏", description = "从收藏列表中移除指定收藏")
    @DeleteMapping("/favorites/{favoriteId}")
    public Result<Void> removeFavorite(
            @Parameter(description = "收藏 ID", required = true) @PathVariable Long favoriteId) {
        conversationService.removeFavorite(favoriteId);
        return Result.success();
    }

    @Operation(summary = "获取用户收藏列表", description = "获取指定用户的所有收藏记录")
    @GetMapping("/favorites")
    public Result<List<ConversationFavorite>> getUserFavorites(
            @Parameter(description = "用户 ID", required = true) @RequestParam Long userId) {
        return Result.success(conversationService.getUserFavorites(userId));
    }

    @Operation(summary = "创建会话模板", description = "创建对话模板，支持公开或私有")
    @PostMapping("/templates")
    public Result<ConversationTemplate> createTemplate(@RequestBody Map<String, Object> params) {
        return Result.success(conversationService.createTemplate(
                (String) params.get("title"),
                (String) params.get("content"),
                (String) params.get("category"),
                Boolean.valueOf(params.get("isPublic").toString()),
                Long.valueOf(params.get("createUserId").toString())));
    }

    @Operation(summary = "获取公开模板", description = "获取所有公开的会话模板")
    @GetMapping("/templates/public")
    public Result<List<ConversationTemplate>> getPublicTemplates() {
        return Result.success(conversationService.getPublicTemplates());
    }

    @Operation(summary = "获取用户模板", description = "获取指定用户创建的会话模板")
    @GetMapping("/templates/user")
    public Result<List<ConversationTemplate>> getUserTemplates(
            @Parameter(description = "用户 ID", required = true) @RequestParam Long userId) {
        return Result.success(conversationService.getUserTemplates(userId));
    }

    @Operation(summary = "使用模板", description = "使用会话模板，增加使用次数")
    @PostMapping("/templates/{templateId}/use")
    public Result<Void> useTemplate(
            @Parameter(description = "模板 ID", required = true) @PathVariable Long templateId) {
        conversationService.useTemplate(templateId);
        return Result.success();
    }

    @Operation(summary = "删除模板", description = "删除指定的会话模板")
    @DeleteMapping("/templates/{templateId}")
    public Result<Void> deleteTemplate(
            @Parameter(description = "模板 ID", required = true) @PathVariable Long templateId) {
        conversationService.deleteTemplate(templateId);
        return Result.success();
    }
}
