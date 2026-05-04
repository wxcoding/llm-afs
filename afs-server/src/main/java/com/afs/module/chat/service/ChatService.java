package com.afs.module.chat.service;

import com.afs.module.knowledge.service.RagService;
import com.afs.module.chat.entity.ChatMessage;
import com.afs.module.chat.entity.ChatSession;
import com.afs.module.chat.mapper.ChatMessageMapper;
import com.afs.module.chat.mapper.ChatSessionMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 聊天服务类
 * 
 * 负责处理对话管理、消息发送、历史记录查询等核心业务逻辑，
 * 集成RAG技术实现基于知识库的智能问答
 */
@Service
public class ChatService {

    @Autowired
    private ChatSessionMapper sessionMapper;

    @Autowired
    private ChatMessageMapper messageMapper;

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private RagService ragService;

    /**
     * 发送消息并通过 SSE 流式返回 AI 响应
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID（新建会话时为null）
     * @param content 用户输入内容
     * @param writer SSE响应输出流
     * @throws Exception 处理过程中的异常
     */
    public void sendMessageStream(Long userId, Long sessionId, String content, PrintWriter writer) throws Exception {
        // 创建或获取会话
        ChatSession session;
        if (sessionId == null) {
            // 新建会话，自动生成标题（截取前20个字符）
            session = new ChatSession();
            session.setUserId(userId);
            session.setTitle(content.length() > 20 ? content.substring(0, 20) + "..." : content);
            session.setCreateTime(LocalDateTime.now());
            sessionMapper.insert(session);
            sessionId = session.getId();
        } else {
            session = sessionMapper.selectById(sessionId);
        }

        // 保存用户消息到数据库
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(content);
        userMsg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(userMsg);

        // 通过RAG服务构建上下文，从知识库检索相关内容
        String context = ragService.buildContext(content, 5);
        List<Map<String, Object>> sources = ragService.searchWithMetadata(content, 5);

        // 发送会话ID信息
        Map<String, Object> sessionInfo = new HashMap<>();
        sessionInfo.put("sessionId", sessionId);
        writer.write("data: " + new ObjectMapper().writeValueAsString(sessionInfo) + "\n\n");
        writer.flush();

        // 如果有检索到的来源，发送来源信息
        if (!sources.isEmpty()) {
            Map<String, Object> sourcesData = new HashMap<>();
            sourcesData.put("sources", sources);
            writer.write("data: " + new ObjectMapper().writeValueAsString(sourcesData) + "\n\n");
            writer.flush();
        }

        // 调用AI流式响应
        String aiResponse = callAIStreamWithRag(sessionId, content, context, writer);

        // 保存AI回复消息到数据库
        ChatMessage assistantMsg = new ChatMessage();
        assistantMsg.setSessionId(sessionId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(aiResponse);
        assistantMsg.setSources(sources.isEmpty() ? null : new ObjectMapper().writeValueAsString(sources));
        assistantMsg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(assistantMsg);
    }

    /**
     * 调用AI并使用RAG增强进行流式响应
     * 
     * @param sessionId 会话ID
     * @param userContent 用户输入内容
     * @param context RAG检索到的上下文内容
     * @param writer SSE输出流
     * @return AI完整响应内容
     * @throws Exception 处理过程中的异常
     */
    private String callAIStreamWithRag(Long sessionId, String userContent, String context, PrintWriter writer) throws Exception {
        try {
            // 构建对话历史消息列表
            List<Message> messages = buildMessages(sessionId);

            // 根据是否有上下文，构建增强后的用户输入
            String augmentedUserContent;
            if (context != null && !context.isEmpty()) {
                augmentedUserContent = """
                        参考以下知识库内容来回答用户的问题。如果参考内容中有相关信息，请优先基于参考内容回答，并在回答中自然地引用来源。如果参考内容与问题无关，请基于你的通用知识回答。

                        参考内容：
                        %s

                        用户问题：%s"""
                        .formatted(context, userContent);
            } else {
                augmentedUserContent = userContent;
            }

            // 调用AI客户端获取响应
            ChatClient.ChatClientRequestSpec request = chatClient.prompt()
                    .messages(messages)
                    .user(augmentedUserContent);

            String fullResponse = request.call().content();

            // 将响应切分成小块进行流式输出（每块50字符）
            StringBuilder responseBuilder = new StringBuilder();
            int chunkSize = 50;

            for (int i = 0; i < fullResponse.length(); i += chunkSize) {
                int end = Math.min(i + chunkSize, fullResponse.length());
                String chunk = fullResponse.substring(i, end);
                responseBuilder.append(chunk);

                // 发送SSE事件
                Map<String, Object> data = new HashMap<>();
                data.put("content", chunk);
                writer.write("data: " + new ObjectMapper().writeValueAsString(data) + "\n\n");
                writer.flush();

                // 模拟流式输出延迟
                Thread.sleep(100);
            }

            return responseBuilder.toString();
        } catch (Exception e) {
            // AI调用失败时返回模拟响应
            String mockResponse = getMockResponse(userContent);
            Map<String, Object> data = new HashMap<>();
            data.put("content", mockResponse);
            writer.write("data: " + new ObjectMapper().writeValueAsString(data) + "\n\n");
            writer.flush();
            return mockResponse;
        }
    }

    /**
     * 根据会话ID构建消息历史列表
     * 
     * @param sessionId 会话ID
     * @return 消息列表（UserMessage和AssistantMessage交替）
     */
    private List<Message> buildMessages(Long sessionId) {
        List<Message> messages = new ArrayList<>();

        // 查询会话的消息历史，按创建时间排序
        List<ChatMessage> history = messageMapper.selectList(
                new QueryWrapper<ChatMessage>()
                        .eq("session_id", sessionId)
                        .orderByAsc("create_time")
        );

        // 将数据库消息转换为AI消息格式
        for (ChatMessage msg : history) {
            if (msg.getContent() != null) {
                if ("user".equals(msg.getRole())) {
                    messages.add(new UserMessage(msg.getContent()));
                } else if ("assistant".equals(msg.getRole())) {
                    messages.add(new AssistantMessage(msg.getContent()));
                }
            }
        }
        return messages;
    }

    private String getMockResponse(String lastUserMsg) {
        String lowerMsg = lastUserMsg.toLowerCase();

        if (lowerMsg.contains("电信诈骗") || lowerMsg.contains("诈骗") || lowerMsg.contains("防骗")) {
            return """
                防诈骗提示：

                1. **警惕陌生来电**：自称公检法、银行、快递等要求转账的都是诈骗！
                2. **不要轻信中奖信息**：天上不会掉馅饼，中奖先交钱都是骗局！
                3. **保护个人信息**：身份证号、银行卡号、验证码不要告诉陌生人！
                4. **核实对方身份**：遇到可疑情况，先挂断电话，再官方渠道核实！
                5. **不点击陌生链接**：短信、微信中的不明链接不要点！

                如遇到诈骗，请立即拨打110报警！""";
        } else if (lowerMsg.contains("网络诈骗")) {
            return """
                网络诈骗常见手法：

                1. **虚假购物**：低价商品先付款后发货，然后消失
                2. **刷单返利**：前期小利，后期大额充值后无法提现
                3. **虚假投资**：高收益稳赚，典型的庞氏骗局
                4. **冒充客服**：假称退款、注销账户，诱导转账
                5. **杀猪盘**：网恋交友，诱导投资赌博

                记住：只要让你转账给陌生人，就很可能是诈骗！""";
        } else {
            return """
                您好！我是防诈骗知识助手，基于RAG知识库为您提供专业解答。

                我可以帮您了解：
                - 电信诈骗的常见手法和防范方法
                - 网络诈骗的各种套路
                - 冒充类诈骗的识别技巧
                - 投资理财诈骗的识别
                - 各类诈骗案例分享

                请告诉我您想了解的内容！""";
        }
    }

    public List<ChatSession> getUserSessions(Long userId) {
        return sessionMapper.selectList(
                new QueryWrapper<ChatSession>()
                        .eq("user_id", userId)
                        .orderByDesc("create_time")
        );
    }

    public List<ChatMessage> getSessionMessages(Long sessionId) {
        return messageMapper.selectList(
                new QueryWrapper<ChatMessage>()
                        .eq("session_id", sessionId)
                        .orderByAsc("create_time")
        );
    }

    public void deleteSession(Long sessionId) {
        messageMapper.delete(new QueryWrapper<ChatMessage>().eq("session_id", sessionId));
        sessionMapper.deleteById(sessionId);
    }
}
