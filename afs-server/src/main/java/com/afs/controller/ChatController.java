package com.afs.controller;

import com.afs.entity.ChatMessage;
import com.afs.entity.ChatSession;
import com.afs.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    public Map<String, Object> send(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = Long.valueOf(params.get("userId").toString());
            Long sessionId = params.get("sessionId") != null ? 
                    Long.valueOf(params.get("sessionId").toString()) : null;
            String content = params.get("content").toString();
            
            Map<String, Object> chatResult = chatService.sendMessage(userId, sessionId, content);
            result.put("success", true);
            result.putAll(chatResult);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
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
