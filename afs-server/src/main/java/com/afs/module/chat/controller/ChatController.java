package com.afs.module.chat.controller;

import com.afs.common.Result;
import com.afs.module.chat.dto.SendMessageDTO;
import com.afs.module.chat.entity.ChatMessage;
import com.afs.module.chat.entity.ChatSession;
import com.afs.module.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "智能对话", description = "聊天对话、会话管理、历史记录等接口，使用 SSE 实现流式响应")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/chat")
@CrossOrigin
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "发送消息并获取流式响应", description = "通过 SSE (Server-Sent Events) 技术实时推送 AI 回复内容")
    @PostMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void send(@RequestBody SendMessageDTO request, HttpServletResponse response) throws Exception {
        // 设置 SSE 响应头
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding("UTF-8");

        // 调用 Service 进行流式响应
        chatService.sendMessageStream(request.getUserId(), request.getSessionId(), request.getContent(), response.getWriter());
    }

    @Operation(summary = "获取用户的所有会话列表")
    @GetMapping("/sessions/{userId}")
    public Result<List<ChatSession>> getSessions(
            @Parameter(description = "用户 ID", required = true) @PathVariable Long userId) {
        return Result.success(chatService.getUserSessions(userId));
    }

    @Operation(summary = "获取会话的聊天历史记录")
    @GetMapping("/history/{sessionId}")
    public Result<List<ChatMessage>> getHistory(
            @Parameter(description = "会话 ID", required = true) @PathVariable Long sessionId) {
        return Result.success(chatService.getSessionMessages(sessionId));
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/session/{sessionId}")
    public Result<Void> deleteSession(
            @Parameter(description = "会话 ID", required = true) @PathVariable Long sessionId) {
        chatService.deleteSession(sessionId);
        return Result.success("删除成功");
    }
}
