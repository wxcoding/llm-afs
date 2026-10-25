package com.afs.module.chat.controller;

import com.afs.security.AuthInterceptor;
import com.afs.module.chat.dto.SendMessageDTO;
import com.afs.module.chat.entity.ChatMessage;
import com.afs.module.chat.entity.ChatSession;
import com.afs.module.chat.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ChatController 单元测试
 *
 * 基于 MockMvc standalone 模式验证 REST 端点的入参传递与响应封装，
 * 流式端点通过方法直调验证 Flux 透传；userId 统一模拟 AuthInterceptor 写入的 request attribute。
 */
class ChatControllerTest {

    private ChatService chatService;
    private ChatController chatController;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        chatService = mock(ChatService.class);
        chatController = new ChatController(chatService);
        mockMvc = MockMvcBuilders.standaloneSetup(chatController).build();
    }

    @Test
    void 流式发送端点从请求属性取userId并透传Flux() {
        SendMessageDTO dto = new SendMessageDTO();
        dto.setSessionId(5L);
        dto.setContent("你好");
        // 模拟 AuthInterceptor 写入的用户身份
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setAttribute(AuthInterceptor.USER_ID_ATTRIBUTE, 1L);

        Flux<ServerSentEvent<String>> flux = Flux.just(ServerSentEvent.builder("{}").build());
        when(chatService.sendMessageStream(1L, 5L, "你好")).thenReturn(flux);

        // 方法直调：返回的 Flux 应原样透传，不重复包装
        assertSame(flux, chatController.send(dto, httpRequest));
        verify(chatService).sendMessageStream(1L, 5L, "你好");
    }

    @Test
    void 获取会话列表返回统一成功结果() throws Exception {
        ChatSession session = new ChatSession();
        session.setId(1L);
        session.setTitle("防骗咨询");
        when(chatService.getUserSessions(1L)).thenReturn(List.of(session));

        mockMvc.perform(get("/api/chat/sessions").requestAttr(AuthInterceptor.USER_ID_ATTRIBUTE, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].title").value("防骗咨询"));
    }

    @Test
    void 获取历史记录传入会话ID与用户ID() throws Exception {
        ChatMessage msg = new ChatMessage();
        msg.setId(10L);
        msg.setRole("user");
        msg.setContent("历史消息");
        when(chatService.getSessionMessages(5L, 1L)).thenReturn(List.of(msg));

        mockMvc.perform(get("/api/chat/history/5").requestAttr(AuthInterceptor.USER_ID_ATTRIBUTE, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].content").value("历史消息"));

        verify(chatService).getSessionMessages(5L, 1L);
    }

    @Test
    void 删除会话端点调用服务层() throws Exception {
        mockMvc.perform(delete("/api/chat/session/5").requestAttr(AuthInterceptor.USER_ID_ATTRIBUTE, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("删除成功"));

        verify(chatService).deleteSession(5L, 1L);
    }

    @Test
    void 重命名会话端点从请求体取标题() throws Exception {
        mockMvc.perform(put("/api/chat/session/5")
                        .requestAttr(AuthInterceptor.USER_ID_ATTRIBUTE, 1L)
                        .contentType("application/json")
                        .content("{\"title\":\"新标题\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("重命名成功"));

        verify(chatService).renameSession(5L, 1L, "新标题");
    }
}
