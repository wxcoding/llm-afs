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

@RestController
@RequestMapping("/api/chat")
@CrossOrigin
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping("/send")
    public void send(@RequestBody Map<String, Object> params, HttpServletResponse response) {
        try {
            Long userId = Long.valueOf(params.get("userId").toString());
            Long sessionId = params.get("sessionId") != null ? 
                    Long.valueOf(params.get("sessionId").toString()) : null;
            String content = params.get("content").toString();
            
            // 设置响应头，支持SSE
            response.setContentType("text/event-stream");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Connection", "keep-alive");
            
            PrintWriter writer = response.getWriter();
            
            // 调用流式方法
            chatService.sendMessageStream(userId, sessionId, content, writer);
            
            // 发送结束信号
            writer.write("data: [DONE]\n\n");
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
}
