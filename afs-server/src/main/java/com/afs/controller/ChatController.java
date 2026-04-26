package com.afs.controller;

import com.afs.entity.ChatMessage;
import com.afs.entity.ChatSession;
import com.afs.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 聊天控制器
 *
 * 提供聊天对话的 REST API 接口，包括发送消息、获取会话列表、历史记录等。
 * 使用 Server-Sent Events（SSE）实现流式响应。
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin
public class ChatController {

    @Autowired
    private ChatService chatService;

    /**
     * 发送消息并获取流式响应
     *
     * 使用 SSE（Server-Sent Events）技术进行流式输出，实时推送 AI 回复内容。
     *
     * @param params   包含 userId、sessionId（可选）、content 的请求体
     * @param response HTTP 响应对象，用于 SSE 输出
     */
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

    /**
     * 获取用户的所有会话列表
     *
     * @param userId 用户 ID
     * @return 会话列表
     */
    @GetMapping("/sessions/{userId}")
    public Map<String, Object> getSessions(@PathVariable Long userId) {
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

    /**
     * 获取会话的聊天历史记录
     *
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    @GetMapping("/history/{sessionId}")
    public Map<String, Object> getHistory(@PathVariable Long sessionId) {
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

    /**
     * 删除会话
     *
     * @param sessionId 会话 ID
     * @return 删除结果
     */
    @DeleteMapping("/session/{sessionId}")
    public Map<String, Object> deleteSession(@PathVariable Long sessionId) {
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
