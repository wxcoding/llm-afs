package com.afs.module.chat.controller;

import com.afs.common.AuthInterceptor;
import com.afs.common.Result;
import com.afs.module.chat.dto.SendMessageDTO;
import com.afs.module.chat.entity.ChatMessage;
import com.afs.module.chat.entity.ChatSession;
import com.afs.module.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Tag(name = "智能对话", description = "聊天对话、会话管理、历史记录等接口，使用 SSE 实现流式响应")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "发送消息并获取流式响应", description = "通过 SSE (Server-Sent Events) 技术实时推送 AI 回复内容")
    @PostMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> send(@RequestBody SendMessageDTO request, HttpServletRequest httpRequest) {
        // 用户身份取自登录令牌，不信任前端传入
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);
        // 返回 Flux 交给 Spring 异步写出，不占用 Servlet 请求线程
        return chatService.sendMessageStream(userId, request.getSessionId(), request.getContent());
    }

    @Operation(summary = "获取当前用户的所有会话列表")
    @GetMapping("/sessions")
    public Result<List<ChatSession>> getSessions(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);
        return Result.success(chatService.getUserSessions(userId));
    }

    @Operation(summary = "获取会话的聊天历史记录")
    @GetMapping("/history/{sessionId}")
    public Result<List<ChatMessage>> getHistory(
            @Parameter(description = "会话 ID", required = true) @PathVariable Long sessionId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);
        return Result.success(chatService.getSessionMessages(sessionId, userId));
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/session/{sessionId}")
    public Result<Void> deleteSession(
            @Parameter(description = "会话 ID", required = true) @PathVariable Long sessionId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);
        chatService.deleteSession(sessionId, userId);
        return Result.success("删除成功");
    }

    @Operation(summary = "重命名会话标题")
    @PutMapping("/session/{sessionId}")
    public Result<Void> renameSession(
            @Parameter(description = "会话 ID", required = true) @PathVariable Long sessionId,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE);
        chatService.renameSession(sessionId, userId, body.get("title"));
        return Result.success("重命名成功");
    }
}
