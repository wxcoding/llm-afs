package com.afs.module.chat.service;

import com.afs.exception.BusinessException;
import com.afs.module.rag.context.RagContextBuilder;
import com.afs.module.rag.search.RagSearchService;
import com.afs.module.chat.entity.ChatMessage;
import com.afs.module.chat.entity.ChatSession;
import com.afs.module.chat.mapper.ChatMessageMapper;
import com.afs.module.chat.mapper.ChatSessionMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 聊天服务类
 *
 * 负责处理对话管理、消息发送、历史记录查询等核心业务逻辑，
 * 集成RAG技术实现基于知识库的智能问答
 *
 * @author guanwx
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ChatService {

    /** 聊天会话数据访问接口 */
    private final ChatSessionMapper sessionMapper;

    /** 聊天消息数据访问接口 */
    private final ChatMessageMapper messageMapper;

    /** AI 对话客户端 */
    private final ChatClient chatClient;

    /** RAG 检索服务 */
    private final RagSearchService ragSearchService;

    /** RAG 上下文构建器 */
    private final RagContextBuilder ragContextBuilder;

    /** JSON 序列化工具 */
    private final ObjectMapper objectMapper;

    /**
     * 发送消息并通过 SSE 流式返回 AI 响应
     *
     * <p>返回 Flux 交给 Spring 异步写出，全程不占用 Servlet 请求线程；
     * 同步准备逻辑（会话、入库、RAG 检索）调度到 boundedElastic 线程池执行。</p>
     *
     * @param userId    用户ID
     * @param sessionId 会话ID（新建会话时为null）
     * @param content   用户输入内容
     * @return SSE 事件流
     */
    public Flux<ServerSentEvent<String>> sendMessageStream(Long userId, Long sessionId, String content) {
        return Flux.defer(() -> prepareAndChat(userId, sessionId, content))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 准备会话与上下文并发起 AI 流式对话
     *
     * @param userId    用户ID
     * @param sessionId 会话ID（为null时自动新建会话）
     * @param content   用户输入内容
     * @return SSE 事件流（头部事件 + AI 内容增量事件）
     */
    private Flux<ServerSentEvent<String>> prepareAndChat(Long userId, Long sessionId, String content) {
        // 创建或获取会话
        if (sessionId == null) {
            // 新建会话，自动生成标题（截取前20个字符）
            ChatSession session = new ChatSession();
            session.setUserId(userId);
            session.setTitle(content.length() > 20 ? content.substring(0, 20) + "..." : content);
            session.setCreateTime(LocalDateTime.now());
            sessionMapper.insert(session);
            sessionId = session.getId();
        }

        // 保存用户消息到数据库
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(content);
        userMsg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(userMsg);

        // 通过RAG服务构建上下文，从知识库检索相关内容（带降级机制）
        String context = "";
        List<Map<String, Object>> sources = new ArrayList<>();
        try {
            sources = ragSearchService.searchWithMetadata(content, 5);
            if (!sources.isEmpty()) {
                context = ragContextBuilder.buildContext(sources);
            }
        } catch (Exception e) {
            log.warn("RAG 检索失败，已降级为纯 AI 对话模式", e);
        }

        // 头部事件：会话ID、来源信息
        List<ServerSentEvent<String>> head = new ArrayList<>();
        head.add(sse(toJson(Map.of("sessionId", sessionId))));
        if (!sources.isEmpty()) {
            head.add(sse(toJson(Map.of("sources", sources))));
        }

        return Flux.concat(Flux.fromIterable(head),
                callAIStreamWithRag(sessionId, content, context, sources));
    }

    /**
     * 调用AI并使用RAG增强进行流式响应
     *
     * @param sessionId   会话ID
     * @param userContent 用户输入内容
     * @param context     RAG检索到的上下文内容
     * @param sources     检索来源（用于随回复一起入库）
     * @return AI 内容增量事件流
     */
    private Flux<ServerSentEvent<String>> callAIStreamWithRag(Long sessionId,
                                                              String userContent,
                                                              String context,
                                                              List<Map<String, Object>> sources) {
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

            messages.add(new UserMessage(augmentedUserContent));

            ChatClient.ChatClientRequestSpec request = chatClient.prompt()
                    .messages(messages);

            StringBuilder responseBuilder = new StringBuilder();
            return request.stream().chatResponse()
                    // 超时兜底：首token及相邻增量间隔超过120秒判定为模型挂起
                    .timeout(Duration.ofSeconds(120))
                    // Reactor 的 map 不允许返回 null，无效块用空串占位交由 filter 过滤
                    .map(chatResponse -> (chatResponse != null && chatResponse.getResult() != null
                            && chatResponse.getResult().getOutput() != null)
                            ? chatResponse.getResult().getOutput().getText()
                            : "")
                    .filter(chunk -> !chunk.isEmpty())
                    .doOnNext(responseBuilder::append)
                    .map(chunk -> sse(toJson(Map.of("content", chunk))))
                    // 兜底：流结束但未产出任何有效内容（如思考型模型仅输出 reasoning_content、响应为空），
                    // 此时不会触发 onErrorResume，需在此降级，避免前端空白
                    .switchIfEmpty(Flux.defer(() -> {
                        log.warn("AI 流结束但未收到有效内容，返回降级响应");
                        String fallback = "抱歉，本次未能获取到有效回复，请稍后重试。";
                        saveAssistantMessage(sessionId, fallback, null);
                        return mockStream(fallback);
                    }))
                    .doOnComplete(() -> saveAssistantMessage(sessionId, responseBuilder.toString(), sources))
                    .onErrorResume(e -> {
                        log.error("AI 调用失败，返回降级响应", e);
                        String mockResponse = getMockResponse(userContent);
                        saveAssistantMessage(sessionId, mockResponse, null);
                        return mockStream(mockResponse);
                    });
        } catch (Exception e) {
            // 构建请求阶段失败同样降级
            log.error("AI 请求构建失败，返回降级响应", e);
            String mockResponse = getMockResponse(userContent);
            saveAssistantMessage(sessionId, mockResponse, null);
            return mockStream(mockResponse);
        }
    }

    /**
     * 降级文案也按小块流式发出：与正常回复一致的 SSE 分块，
     * 避免单条超长 JSON 事件被网络分片截断导致前端解析失败。
     */
    private Flux<ServerSentEvent<String>> mockStream(String mockText) {
        List<ServerSentEvent<String>> events = new ArrayList<>();
        for (int i = 0; i < mockText.length(); i += 50) {
            int end = Math.min(i + 50, mockText.length());
            events.add(sse(toJson(Map.of("content", mockText.substring(i, end)))));
        }
        return Flux.fromIterable(events);
    }

    /**
     * 将 AI 回复消息保存到数据库
     *
     * @param sessionId 会话ID
     * @param content   AI 回复内容（为空时不保存）
     * @param sources   RAG 检索来源，序列化为 JSON 后随消息入库，可为null
     */
    private void saveAssistantMessage(Long sessionId, String content, List<Map<String, Object>> sources) {
        if (content == null || content.isEmpty()) {
            return;
        }
        ChatMessage assistantMsg = new ChatMessage();
        assistantMsg.setSessionId(sessionId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(content);
        assistantMsg.setSources(sources == null || sources.isEmpty() ? null : toJson(sources));
        assistantMsg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(assistantMsg);
    }

    /**
     * 将负载字符串包装为 SSE 事件对象
     *
     * @param payload 事件负载内容
     * @return SSE 事件对象
     */
    private ServerSentEvent<String> sse(String payload) {
        return ServerSentEvent.builder(payload).build();
    }

    /**
     * 将对象序列化为 JSON 字符串
     *
     * @param value 待序列化的对象
     * @return JSON 字符串
     * @throws IllegalStateException 序列化失败时抛出
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON 序列化失败", e);
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

        // 只取最近10条历史，避免长会话导致 prompt 膨胀、首 token 变慢
        List<ChatMessage> history = messageMapper.selectList(
                new QueryWrapper<ChatMessage>()
                        .eq("session_id", sessionId)
                        .orderByDesc("create_time")
                        .last("LIMIT 10")
        );

        // 反转最近十条历史消息，保证最近十条按语义顺序，由前到后的对话
        Collections.reverse(history);

        // 将数据库消息转换为AI消息格式
        for (ChatMessage msg : history) {
            if (msg.getContent() == null) {
                continue;
            }
            if ("user".equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else if ("assistant".equals(msg.getRole())) {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }
        return messages;
    }

    /**
     * 根据用户最后一条消息生成降级回复文案
     *
     * @param lastUserMsg 用户最后一条消息内容
     * @return 匹配的防诈骗提示文案
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
     * 查询指定用户的所有会话列表
     *
     * @param userId 用户ID
     * @return 会话列表，按创建时间倒序排列
     */
    public List<ChatSession> getUserSessions(Long userId) {
        return sessionMapper.selectList(
                new QueryWrapper<ChatSession>()
                        .eq("user_id", userId)
                        .orderByDesc("create_time")
        );
    }

    /**
     * 查询指定会话的全部消息记录（校验会话归属）
     *
     * @param sessionId 会话ID
     * @param userId    用户ID，用于校验会话归属
     * @return 消息列表，按创建时间升序排列
     */
    public List<ChatMessage> getSessionMessages(Long sessionId, Long userId) {
        requireOwnedSession(sessionId, userId);
        return messageMapper.selectList(
                new QueryWrapper<ChatMessage>()
                        .eq("session_id", sessionId)
                        .orderByAsc("create_time")
        );
    }

    /**
     * 删除指定会话及其全部消息（校验会话归属）
     *
     * @param sessionId 会话ID
     * @param userId    用户ID，用于校验会话归属
     */
    public void deleteSession(Long sessionId, Long userId) {
        requireOwnedSession(sessionId, userId);
        messageMapper.delete(new QueryWrapper<ChatMessage>().eq("session_id", sessionId));
        sessionMapper.deleteById(sessionId);
    }

    /**
     * 校验会话存在且属于当前用户，防止水平越权访问他人会话
     */
    private ChatSession requireOwnedSession(Long sessionId, Long userId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw BusinessException.notFound("会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            throw BusinessException.forbidden("无权访问该会话");
        }
        return session;
    }

    /**
     * 重命名指定会话的标题（校验会话归属，标题最长50字符）
     *
     * @param sessionId 会话ID
     * @param userId    用户ID，用于校验会话归属
     * @param title     新的会话标题
     */
    public void renameSession(Long sessionId, Long userId, String title) {
        ChatSession session = requireOwnedSession(sessionId, userId);
        String trimmed = title == null ? "" : title.trim();
        if (trimmed.isEmpty()) {
            throw BusinessException.badRequest("会话标题不能为空");
        }
        if (trimmed.length() > 50) {
            trimmed = trimmed.substring(0, 50);
        }
        session.setTitle(trimmed);
        sessionMapper.updateById(session);
    }
}
