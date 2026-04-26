package com.afs.service;

import com.afs.entity.ChatMessage;
import com.afs.entity.ChatSession;
import com.afs.mapper.ChatMessageMapper;
import com.afs.mapper.ChatSessionMapper;
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
 * 聊天服务
 *
 * 处理用户与 AI 助手的对话逻辑，包括：
 * - 消息的发送与流式响应（SSE）
 * - 对话历史的存储与上下文管理
 * - RAG 检索增强，将知识库内容融入 AI 回复
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
     * 发送消息并流式返回响应
     *
     * 使用 Server-Sent Events（SSE）实现流式输出，将 AI 响应分块推送至前端。
     * 如果未提供 sessionId，则自动创建新会话。
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID，null 表示新建会话
     * @param content   用户消息内容
     * @param writer    HTTP 响应输出流，用于 SSE 推送
     */
    public void sendMessageStream(Long userId, Long sessionId, String content, PrintWriter writer) throws Exception {
        ChatSession session;
        // 处理会话：如果没有会话ID则创建新会话
        if (sessionId == null) {
            session = new ChatSession();
            session.setUserId(userId);
            // 将会话标题设置为消息内容的前20个字符
            session.setTitle(content.length() > 20 ? content.substring(0, 20) + "..." : content);
            session.setCreateTime(LocalDateTime.now());
            sessionMapper.insert(session);
            sessionId = session.getId();
        } else {
            // 否则获取现有会话
            session = sessionMapper.selectById(sessionId);
        }

        // 保存用户消息到数据库
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(content);
        userMsg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(userMsg);

        // 通过RAG服务构建上下文和获取相关知识来源
        String context = ragService.buildContext(content, 5);
        List<Map<String, Object>> sources = ragService.searchWithMetadata(content, 5);

        // 发送会话信息到前端
        Map<String, Object> sessionInfo = new HashMap<>();
        sessionInfo.put("sessionId", sessionId);
        writer.write("data: " + new ObjectMapper().writeValueAsString(sessionInfo) + "\n\n");
        writer.flush();

        // 如果有知识来源，发送来源信息到前端
        if (!sources.isEmpty()) {
            Map<String, Object> sourcesData = new HashMap<>();
            sourcesData.put("sources", sources);
            writer.write("data: " + new ObjectMapper().writeValueAsString(sourcesData) + "\n\n");
            writer.flush();
        }

        // 调用AI并获取流式响应
        String aiResponse = callAIStreamWithRag(sessionId, content, context, writer);

        // 保存AI回复到数据库
        ChatMessage assistantMsg = new ChatMessage();
        assistantMsg.setSessionId(sessionId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(aiResponse);
        // 保存知识来源信息（如果有）
        assistantMsg.setSources(sources.isEmpty() ? null : new ObjectMapper().writeValueAsString(sources));
        assistantMsg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(assistantMsg);
    }

    /**
     * 调用 AI 并流式返回响应（带 RAG 上下文）
     *
     * 将用户消息与知识库上下文结合，调用 AI 生成回复，
     * 并通过 SSE 分块推送至前端。如果 AI 调用失败，启用降级 mock 回复。
     *
     * @param sessionId      会话 ID
     * @param userContent    用户原始消息
     * @param context        RAG 检索构建的参考上下文
     * @param writer         HTTP 响应输出流
     * @return AI 完整回复文本
     */
    private String callAIStreamWithRag(Long sessionId, String userContent, String context, PrintWriter writer) throws Exception {
        try {
            // 构建对话历史消息列表
            List<Message> messages = buildMessages(sessionId);

            // 构建增强的用户消息（包含知识库上下文）
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

            // 构建AI请求
            ChatClient.ChatClientRequestSpec request = chatClient.prompt()
                    .messages(messages)
                    .user(augmentedUserContent);

            // 调用AI并获取完整响应
            String fullResponse = request.call().content();

            // 流式输出响应
            StringBuilder responseBuilder = new StringBuilder();
            int chunkSize = 50; // 每次发送50个字符

            // 分块发送响应内容
            for (int i = 0; i < fullResponse.length(); i += chunkSize) {
                int end = Math.min(i + chunkSize, fullResponse.length());
                String chunk = fullResponse.substring(i, end);
                responseBuilder.append(chunk);

                // 构建SSE数据格式
                Map<String, Object> data = new HashMap<>();
                data.put("content", chunk);
                writer.write("data: " + new ObjectMapper().writeValueAsString(data) + "\n\n");
                writer.flush();

                // 模拟流式效果，增加100ms延迟
                Thread.sleep(100);
            }

            return responseBuilder.toString();
        } catch (Exception e) {
            // AI调用失败时，返回降级的mock回复
            String mockResponse = getMockResponse(userContent);
            Map<String, Object> data = new HashMap<>();
            data.put("content", mockResponse);
            writer.write("data: " + new ObjectMapper().writeValueAsString(data) + "\n\n");
            writer.flush();
            return mockResponse;
        }
    }

    /**
     * 构建对话历史消息列表
     *
     * 从数据库加载当前会话的所有历史消息，构建 AI 对话上下文。
     *
     * @param sessionId 会话 ID
     * @return 按时间顺序排列的消息列表
     */
    private List<Message> buildMessages(Long sessionId) {
        List<Message> messages = new ArrayList<>();

        // 查询会话的所有历史消息（按时间顺序）
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

    /**
     * 获取 Mock 回复（降级策略）
     *
     * 当 AI 服务不可用时，根据用户消息关键词返回预设的防诈骗回复。
     *
     * @param lastUserMsg 用户最后一条消息
     * @return 预设的回复文本
     */
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

    /**
     * 获取用户的所有会话列表
     *
     * @param userId 用户 ID
     * @return 该用户的所有会话列表
     */
    public List<ChatSession> getUserSessions(Long userId) {
        return sessionMapper.selectList(
                new QueryWrapper<ChatSession>()
                        .eq("user_id", userId)
                        .orderByDesc("create_time")
        );
    }

    /**
     * 获取会话的所有消息
     *
     * @param sessionId 会话 ID
     * @return 该会话的所有消息列表
     */
    public List<ChatMessage> getSessionMessages(Long sessionId) {
        return messageMapper.selectList(
                new QueryWrapper<ChatMessage>()
                        .eq("session_id", sessionId)
                        .orderByAsc("create_time")
        );
    }

    /**
     * 删除会话及其所有消息
     *
     * @param sessionId 会话 ID
     */
    public void deleteSession(Long sessionId) {
        messageMapper.delete(new QueryWrapper<ChatMessage>().eq("session_id", sessionId));
        sessionMapper.deleteById(sessionId);
    }
}
