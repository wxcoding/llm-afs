package com.afs.module.chat.controller;

import com.afs.module.chat.entity.ChatMessage;
import com.afs.module.chat.entity.ChatSession;
import com.afs.module.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "智能对话", description = "聊天对话、会话管理、历史记录等接口，使用 SSE 实现流式响应")
@RestController
@RequestMapping("/api/chat")
@CrossOrigin
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Operation(summary = "发送消息并获取流式响应", description = "使用 SSE（Server-Sent Events）技术进行流式输出，实时推送 AI 回复内容")
    @PostMapping("/send")
    public void send(@RequestBody Map<String, Object> params, HttpServletResponse response) {
        try {
            Long userId = Long.valueOf(params.get("userId").toString());
            Long sessionId = params.get("sessionId") != null ?
                    Long.valueOf(params.get("sessionId").toString()) : null;
            String content = params.get("content").toString();

            response.setContentType("text/event-stream");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Connection", "keep-alive");

            PrintWriter writer = response.getWriter();

            chatService.sendMessageStream(userId, sessionId, content, writer);

            writer.write("data: [DONE]\n\n");
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Operation(summary = "获取用户的所有会话列表")
    @GetMapping("/sessions/{userId}")
    public Map<String, Object> getSessions(
            @Parameter(description = "用户 ID", required = true) @PathVariable Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<ChatSession> sessions = chatService.getUserSessions(userId);
            result.put("success", true);
            result.put("sessions", sessions);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @Operation(summary = "获取会话的聊天历史记录")
    @GetMapping("/history/{sessionId}")
    public Map<String, Object> getHistory(
            @Parameter(description = "会话 ID", required = true) @PathVariable Long sessionId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<ChatMessage> messages = chatService.getSessionMessages(sessionId);
            result.put("success", true);
            result.put("messages", messages);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/session/{sessionId}")
    public Map<String, Object> deleteSession(
            @Parameter(description = "会话 ID", required = true) @PathVariable Long sessionId) {
        Map<String, Object> result = new HashMap<>();
        try {
            chatService.deleteSession(sessionId);
            result.put("success", true);
            result.put("message", "删除成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
