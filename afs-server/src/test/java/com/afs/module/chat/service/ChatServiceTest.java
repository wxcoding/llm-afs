package com.afs.module.chat.service;

import com.afs.exception.BusinessException;
import com.afs.module.chat.entity.ChatMessage;
import com.afs.module.chat.entity.ChatSession;
import com.afs.module.chat.mapper.ChatMessageMapper;
import com.afs.module.chat.mapper.ChatSessionMapper;
import com.afs.module.rag.context.RagContextBuilder;
import com.afs.module.rag.search.RagSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ChatService 单元测试
 *
 * 覆盖流式对话（正常输出、空块过滤、RAG 降级、AI 异常降级、空流兜底）、
 * 会话管理（新建/列表/删除/重命名）、消息历史与会话归属校验等业务分支。
 */
class ChatServiceTest {

    private ChatSessionMapper sessionMapper;
    private ChatMessageMapper messageMapper;
    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.StreamResponseSpec streamSpec;
    private RagSearchService ragSearchService;
    private RagContextBuilder ragContextBuilder;
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        sessionMapper = mock(ChatSessionMapper.class);
        messageMapper = mock(ChatMessageMapper.class);
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        streamSpec = mock(ChatClient.StreamResponseSpec.class);
        ragSearchService = mock(RagSearchService.class);
        ragContextBuilder = mock(RagContextBuilder.class);
        chatService = new ChatService(sessionMapper, messageMapper, chatClient,
                ragSearchService, ragContextBuilder, new ObjectMapper());
    }

    // 打桩 ChatClient 调用链，返回指定的 AI 响应流
    private void stubChatClient(Flux<ChatResponse> responses) {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.messages(anyList())).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(streamSpec);
        when(streamSpec.chatResponse()).thenReturn(responses);
    }

    private static ChatResponse chunk(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    // 拼接流中所有 content 事件的文本
    private static String joinContent(List<ServerSentEvent<String>> events) {
        StringBuilder sb = new StringBuilder();
        for (ServerSentEvent<String> event : events) {
            String data = event.data();
            if (data != null && data.startsWith("{\"content\"")) {
                sb.append(data.replaceAll(".*\"content\":\"", "").replaceAll("\"}", ""));
            }
        }
        return sb.toString();
    }

    // 历史打桩：默认无历史消息
    private void stubEmptyHistory() {
        when(messageMapper.selectList(any())).thenReturn(new ArrayList<>());
    }

    @Test
    void 新建会话时自动插入会话并截断标题() {
        // 模拟 MyBatis-Plus 插入后回填主键
        when(sessionMapper.insert(any(ChatSession.class))).thenAnswer(inv -> {
            ChatSession s = inv.getArgument(0);
            s.setId(99L);
            return 1;
        });
        when(ragSearchService.searchWithMetadata(anyString(), eq(5))).thenReturn(new ArrayList<>());
        stubEmptyHistory();
        stubChatClient(Flux.just(chunk("你好")));

        String longContent = "这是一个非常非常非常长的用户输入内容用于测试标题自动截断";
        List<ServerSentEvent<String>> events = chatService
                .sendMessageStream(1L, null, longContent)
                .collectList().block();

        // 校验插入的会话：标题截断为前20字符加省略号
        ArgumentCaptor<ChatSession> sessionCaptor = ArgumentCaptor.forClass(ChatSession.class);
        verify(sessionMapper).insert(sessionCaptor.capture());
        ChatSession saved = sessionCaptor.getValue();
        assertEquals(1L, saved.getUserId());
        assertEquals(23, saved.getTitle().length());
        assertTrue(saved.getTitle().endsWith("..."));

        // 头部事件应携带回填后的会话ID
        assertEquals("{\"sessionId\":99}", events.get(0).data());
    }

    @Test
    void 正常流式输出多个块并拼接且空块被过滤() {
        List<Map<String, Object>> sources = new ArrayList<>();
        sources.add(Map.of("content", "防骗知识", "title", "常识", "source", "知识库"));
        when(ragSearchService.searchWithMetadata("如何防骗", 5)).thenReturn(sources);
        when(ragContextBuilder.buildContext(sources)).thenReturn("CTX");
        stubEmptyHistory();
        // 第二个块为空串、第三个块无生成结果，均应被过滤
        ChatResponse emptyResult = new ChatResponse(new ArrayList<>());
        stubChatClient(Flux.just(chunk("你好"), chunk(""), emptyResult, chunk("世界")));

        List<ServerSentEvent<String>> events = chatService
                .sendMessageStream(2L, 5L, "如何防骗")
                .collectList().block();

        // 头部两个事件：sessionId + sources，之后是内容块
        assertEquals("{\"sessionId\":5}", events.get(0).data());
        assertTrue(events.get(1).data().contains("\"sources\""));
        assertEquals(4, events.size());
        assertEquals("你好世界", joinContent(events));

        // 用户消息与助手回复各入库一次，助手消息带 sources JSON
        ArgumentCaptor<ChatMessage> msgCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messageMapper, times(2)).insert(msgCaptor.capture());
        ChatMessage assistantMsg = msgCaptor.getAllValues().get(1);
        assertEquals("assistant", assistantMsg.getRole());
        assertEquals("你好世界", assistantMsg.getContent());
        assertTrue(assistantMsg.getSources().contains("防骗知识"));
    }

    @Test
    void RAG检索失败时降级为纯AI对话且不发送sources事件() {
        when(ragSearchService.searchWithMetadata(anyString(), eq(5)))
                .thenThrow(new IllegalStateException("RAG 检索失败"));
        stubEmptyHistory();
        stubChatClient(Flux.just(chunk("通用回答")));

        List<ServerSentEvent<String>> events = chatService
                .sendMessageStream(1L, 7L, "随便聊聊")
                .collectList().block();

        // 仅有 sessionId 头事件 + 一个内容块
        assertEquals(2, events.size());
        assertEquals("{\"sessionId\":7}", events.get(0).data());
        assertEquals("通用回答", joinContent(events));
    }

    @Test
    void AI流为空时返回兜底降级文案() {
        when(ragSearchService.searchWithMetadata(anyString(), eq(5))).thenReturn(new ArrayList<>());
        stubEmptyHistory();
        stubChatClient(Flux.empty());

        List<ServerSentEvent<String>> events = chatService
                .sendMessageStream(1L, 8L, "你好")
                .collectList().block();

        String content = joinContent(events);
        assertEquals("抱歉，本次未能获取到有效回复，请稍后重试。", content);

        // 兜底文案也应作为助手消息入库
        ArgumentCaptor<ChatMessage> msgCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messageMapper, times(2)).insert(msgCaptor.capture());
        assertEquals(content, msgCaptor.getAllValues().get(1).getContent());
    }

    @Test
    void AI调用异常时返回降级文案并入库() {
        when(ragSearchService.searchWithMetadata(anyString(), eq(5))).thenReturn(new ArrayList<>());
        stubEmptyHistory();
        stubChatClient(Flux.error(new RuntimeException("AI 服务不可用")));

        List<ServerSentEvent<String>> events = chatService
                .sendMessageStream(1L, 9L, "电信诈骗怎么办")
                .collectList().block();

        // 命中"电信诈骗"关键词的降级文案
        String content = joinContent(events);
        assertTrue(content.contains("防诈骗提示"));

        ArgumentCaptor<ChatMessage> msgCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messageMapper, times(2)).insert(msgCaptor.capture());
        ChatMessage assistantMsg = msgCaptor.getAllValues().get(1);
        assertEquals("assistant", assistantMsg.getRole());
        assertNull(assistantMsg.getSources());
    }

    @Test
    void 请求构建阶段异常时同样走降级路径() {
        when(ragSearchService.searchWithMetadata(anyString(), eq(5))).thenReturn(new ArrayList<>());
        stubEmptyHistory();
        // chatClient.prompt() 直接抛异常，验证外层 catch 降级
        when(chatClient.prompt()).thenThrow(new IllegalStateException("构建失败"));

        List<ServerSentEvent<String>> events = chatService
                .sendMessageStream(1L, 10L, "网络诈骗有哪些")
                .collectList().block();

        // "网络诈骗有哪些" 含"诈骗"关键词，按关键词优先级命中第一条通用防骗文案
        // （"网络诈骗"分支因判断顺序实际不可达，属主代码既有行为，此处按实际断言）
        assertTrue(joinContent(events).contains("防诈骗提示"));
    }

    @Test
    void 构建消息历史时逆序转换并跳过空内容() {
        when(ragSearchService.searchWithMetadata(anyString(), eq(5))).thenReturn(new ArrayList<>());
        // mapper 返回的是按时间倒序的历史（含一条空内容消息）
        ChatMessage m1 = history("assistant", "回复2");
        ChatMessage m2 = history("user", null);
        ChatMessage m3 = history("assistant", "回复1");
        ChatMessage m4 = history("user", "提问1");
        when(messageMapper.selectList(any())).thenReturn(new ArrayList<>(List.of(m1, m2, m3, m4)));
        stubChatClient(Flux.just(chunk("ok")));

        chatService.sendMessageStream(1L, 11L, "最新问题").collectList().block();

        // 捕获传给 AI 的消息列表：历史被反转为正序，空内容被跳过，最后是增强后的用户消息
        ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
        verify(requestSpec).messages(captor.capture());
        List<Message> messages = captor.getValue();
        assertEquals(4, messages.size());
        assertInstanceOf(UserMessage.class, messages.get(0));
        assertEquals("提问1", messages.get(0).getText());
        assertInstanceOf(AssistantMessage.class, messages.get(1));
        assertEquals("回复1", messages.get(1).getText());
        assertInstanceOf(AssistantMessage.class, messages.get(2));
        assertEquals("回复2", messages.get(2).getText());
        // 无 RAG 上下文时直接使用原始输入
        assertEquals("最新问题", messages.get(3).getText());
    }

    @Test
    void 有RAG上下文时用户消息被增强包装() {
        List<Map<String, Object>> sources = new ArrayList<>();
        sources.add(Map.of("content", "防骗知识"));
        when(ragSearchService.searchWithMetadata(anyString(), eq(5))).thenReturn(sources);
        when(ragContextBuilder.buildContext(sources)).thenReturn("CTX-CONTENT");
        stubEmptyHistory();
        stubChatClient(Flux.just(chunk("ok")));

        chatService.sendMessageStream(1L, 12L, "原始问题").collectList().block();

        ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
        verify(requestSpec).messages(captor.capture());
        List<Message> messages = captor.getValue();
        Message last = messages.get(messages.size() - 1);
        assertInstanceOf(UserMessage.class, last);
        assertTrue(last.getText().contains("CTX-CONTENT"));
        assertTrue(last.getText().contains("原始问题"));
    }

    @Test
    void 流式接口使用StepVerifier验证事件序列() {
        when(ragSearchService.searchWithMetadata(anyString(), eq(5))).thenReturn(new ArrayList<>());
        stubEmptyHistory();
        stubChatClient(Flux.just(chunk("块1"), chunk("块2")));

        StepVerifier.create(chatService.sendMessageStream(1L, 13L, "问题"))
                .expectNextMatches(e -> "{\"sessionId\":13}".equals(e.data()))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void 获取用户会话列表按时间倒序() {
        ChatSession s = new ChatSession();
        s.setId(1L);
        when(sessionMapper.selectList(any())).thenReturn(List.of(s));

        List<ChatSession> result = chatService.getUserSessions(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void 获取本人会话消息成功() {
        ChatSession session = ownedSession(5L, 1L);
        when(sessionMapper.selectById(5L)).thenReturn(session);
        ChatMessage msg = history("user", "你好");
        when(messageMapper.selectList(any())).thenReturn(List.of(msg));

        List<ChatMessage> result = chatService.getSessionMessages(5L, 1L);

        assertEquals(1, result.size());
        assertEquals("你好", result.get(0).getContent());
    }

    @Test
    void 会话不存在时抛NotFound异常() {
        when(sessionMapper.selectById(5L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> chatService.getSessionMessages(5L, 1L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("会话不存在", ex.getMessage());
    }

    @Test
    void 访问他人会话时抛Forbidden异常() {
        // 会话归属用户2，当前用户1越权访问
        when(sessionMapper.selectById(5L)).thenReturn(ownedSession(5L, 2L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> chatService.getSessionMessages(5L, 1L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void 删除本人会话时级联删除消息() {
        when(sessionMapper.selectById(5L)).thenReturn(ownedSession(5L, 1L));

        chatService.deleteSession(5L, 1L);

        verify(messageMapper).delete(any());
        verify(sessionMapper).deleteById(5L);
    }

    @Test
    void 删除他人会话被拒绝且不执行删除() {
        when(sessionMapper.selectById(5L)).thenReturn(ownedSession(5L, 2L));

        assertThrows(BusinessException.class, () -> chatService.deleteSession(5L, 1L));
        verify(messageMapper, never()).delete(any());
        verify(sessionMapper, never()).deleteById(5L);
    }

    @Test
    void 重命名会话成功并去除首尾空格() {
        ChatSession session = ownedSession(5L, 1L);
        when(sessionMapper.selectById(5L)).thenReturn(session);

        chatService.renameSession(5L, 1L, "  新标题  ");

        assertEquals("新标题", session.getTitle());
        verify(sessionMapper).updateById(session);
    }

    @Test
    void 重命名会话标题超过50字时截断() {
        ChatSession session = ownedSession(5L, 1L);
        when(sessionMapper.selectById(5L)).thenReturn(session);
        String longTitle = "标".repeat(60);

        chatService.renameSession(5L, 1L, longTitle);

        assertEquals(50, session.getTitle().length());
        verify(sessionMapper).updateById(session);
    }

    @Test
    void 重命名空标题或null标题时抛BadRequest() {
        when(sessionMapper.selectById(5L)).thenReturn(ownedSession(5L, 1L));

        BusinessException ex1 = assertThrows(BusinessException.class,
                () -> chatService.renameSession(5L, 1L, "   "));
        assertEquals(HttpStatus.BAD_REQUEST, ex1.getStatus());

        BusinessException ex2 = assertThrows(BusinessException.class,
                () -> chatService.renameSession(5L, 1L, null));
        assertEquals(HttpStatus.BAD_REQUEST, ex2.getStatus());

        verify(sessionMapper, never()).updateById(any());
    }

    private static ChatMessage history(String role, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setRole(role);
        msg.setContent(content);
        return msg;
    }

    private static ChatSession ownedSession(Long id, Long userId) {
        ChatSession session = new ChatSession();
        session.setId(id);
        session.setUserId(userId);
        return session;
    }
}
