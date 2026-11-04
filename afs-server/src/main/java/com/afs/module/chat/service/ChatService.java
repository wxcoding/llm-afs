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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
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

    /** 来源展示的相关度阈值：低于该值的检索结果不作为"参考来源"展示给用户 */
    @Value("${rag.citation.min-score:0.35}")
    private double citationMinScore;

    /** 模型不可用时，降级回答最多摘录的知识条目数（只给最高分一条，避免半成品答案被当成完整回答） */
    private static final int MAX_EXCERPT_SOURCES = 1;

    /** 降级回答中单条摘录的最大字符数 */
    private static final int MAX_EXCERPT_CHARS = 300;

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
        List<Map<String, Object>> displaySources = new ArrayList<>();
        try {
            sources = ragSearchService.searchWithMetadata(content, 5);
            if (!sources.isEmpty()) {
                context = ragContextBuilder.buildContext(sources);
                // 上下文仍使用全部召回内容，展示层只保留相关度达标的部分，避免低分结果被当作权威引用
                displaySources = filterByRelevance(sources, citationMinScore);
                log.info("RAG 引用门控 - 召回: {}, 达标: {}, 阈值: {}",
                        sources.size(), displaySources.size(), citationMinScore);
            }
        } catch (Exception e) {
            log.warn("RAG 检索失败，已降级为纯 AI 对话模式", e);
        }

        // 头部事件：会话ID + 是否有可引用来源
        // 来源本身在首个内容块到达时才下发，避免 AI 调用失败时"先显示来源又立刻清空"
        List<ServerSentEvent<String>> head = new ArrayList<>();
        head.add(sse(toJson(Map.of("sessionId", sessionId, "noEvidence", displaySources.isEmpty()))));

        return Flux.concat(Flux.fromIterable(head),
                callAIStreamWithRag(sessionId, content, context, displaySources));
    }

    /**
     * 按展示阈值过滤检索来源。
     *
     * <p>仅在分数可信（0~1）且低于阈值时丢弃；分数未知（-1，如重排关闭或重排降级）时保留，
     * 保证任何情况下都不会因为拿不到分数而误杀正常召回。</p>
     *
     * @param sources  检索来源列表
     * @param minScore 展示阈值
     * @return 可用于展示的来源列表
     */
    private List<Map<String, Object>> filterByRelevance(List<Map<String, Object>> sources, double minScore) {
        return sources.stream()
                .filter(source -> {
                    Object score = source.get("score");
                    if (!(score instanceof Number number)) {
                        return true;
                    }
                    double value = number.doubleValue();
                    return value < 0 || value >= minScore;
                })
                .toList();
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
        // 降级时只摘录排名最高的一条，来源同步裁剪，避免"正文引一条、来源列五条"
        List<Map<String, Object>> excerptSources = sources.stream()
                .limit(MAX_EXCERPT_SOURCES)
                .toList();
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
            // 来源事件推迟到首个内容块到达时下发：AI 调用失败时不会先展示来源再立刻清空
            AtomicBoolean sourcesSent = new AtomicBoolean(false);
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
                    .flatMap(event -> {
                        if (sourcesSent.compareAndSet(false, true) && sources != null && !sources.isEmpty()) {
                            return Flux.just(sse(toJson(Map.of("sources", sources))), event);
                        }
                        return Flux.just(event);
                    })
                    // 兜底：流结束但未产出任何有效内容（如思考型模型仅输出 reasoning_content、响应为空），
                    // 此时不会触发 onErrorResume，需在此降级，避免前端空白
                    .switchIfEmpty(Flux.defer(() -> {
                        log.warn("AI 流结束但未收到有效内容，返回降级响应");
                        String fallback = buildDegradedAnswer(resolveDegradedMessage(null), excerptSources);
                        saveAssistantMessage(sessionId, fallback, excerptSources, true);
                        return degradedStream(fallback, excerptSources);
                    }))
                    .doOnComplete(() -> saveAssistantMessage(sessionId, responseBuilder.toString(), sources, false))
                    .onErrorResume(e -> {
                        logAiError(e);
                        String fallback = buildDegradedAnswer(resolveDegradedMessage(e), excerptSources);
                        saveAssistantMessage(sessionId, fallback, excerptSources, true);
                        return degradedStream(fallback, excerptSources);
                    });
        } catch (Exception e) {
            // 构建请求阶段失败同样降级
            logAiError(e);
            String fallback = buildDegradedAnswer(resolveDegradedMessage(e), excerptSources);
            saveAssistantMessage(sessionId, fallback, excerptSources, true);
            return degradedStream(fallback, excerptSources);
        }
    }

    /**
     * 解析降级文案：模型未开通/欠费/鉴权失败时给出可操作的提示，
     * 其余异常返回通用失败提示。
     */
    private String resolveDegradedMessage(Throwable e) {
        if (e instanceof WebClientResponseException httpError) {
            String body = httpError.getResponseBodyAsString();
            if (body.contains("Unpurchased")) {
                return "抱歉，问答模型服务暂未开通（账户欠费或模型未购买），请联系管理员处理后再试。";
            }
            int status = httpError.getStatusCode().value();
            if (status == 401 || status == 403) {
                return "抱歉，问答模型鉴权失败，请联系管理员检查模型配置。";
            }
        }
        return "抱歉，本次未能生成回答，请稍后重试。如遇诈骗，请立即拨打 96110。";
    }

    /**
     * 构造降级回答。
     *
     * <p>若本次有相关度达标的检索结果，则在故障提示后直接摘录知识库原文，作为模型不可用时的
     * 兜底答案；没有达标结果时只返回故障提示，避免把无关内容当作答案。</p>
     *
     * <p>摘录内容固定标注"未经模型总结改写"，避免用户把原文片段误认为模型结论。</p>
     *
     * @param hint    故障提示文案
     * @param sources 已通过相关度过滤的检索来源
     * @return 降级回答文本
     */
    private String buildDegradedAnswer(String hint, List<Map<String, Object>> sources) {
        if (sources == null || sources.isEmpty()) {
            return hint;
        }
        StringBuilder answer = new StringBuilder(hint);
        answer.append("\n\n以下内容直接摘自知识库原文（未经模型总结改写）：\n\n");
        int appended = 0;
        for (Map<String, Object> source : sources) {
            if (appended >= MAX_EXCERPT_SOURCES) {
                break;
            }
            String excerpt = normalizeExcerpt(String.valueOf(source.getOrDefault("content", "")));
            if (excerpt.isEmpty()) {
                continue;
            }
            String title = String.valueOf(source.getOrDefault("title", "未命名"));
            // 正文自身已带标题时不再重复加，避免出现两个相同的【标题】
            if (!excerpt.startsWith("【" + title + "】")) {
                answer.append("【").append(title).append("】\n");
            }
            answer.append(excerpt).append("\n\n");
            appended++;
        }
        if (appended == 0) {
            return hint;
        }
        answer.append("如需完整解答，请稍后重试。");
        // 通用故障提示已包含 96110，避免同一条消息里重复
        if (!hint.contains("96110")) {
            answer.append("如遇诈骗，请立即拨打 96110。");
        }
        return answer.toString();
    }

    /**
     * 规整摘录文本：压缩行内空白，保留换行（避免把列表挤成一整段），并截断到最大长度。
     */
    private String normalizeExcerpt(String raw) {
        String text = raw.replace("\r\n", "\n")
                .replaceAll("[ \\t\\u00A0]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{2,}", "\n")
                .trim();
        if (text.length() > MAX_EXCERPT_CHARS) {
            return text.substring(0, MAX_EXCERPT_CHARS) + "…";
        }
        return text;
    }

    /**
     * 打印 AI 调用失败的详细信息。
     *
     * <p>HTTP 异常（如 403/401/429）额外打印响应体，响应体里通常带具体的错误码
     * （模型未授权、模型名错误、额度不足等），比堆栈更能定位问题。</p>
     */
    private void logAiError(Throwable e) {
        if (e instanceof WebClientResponseException httpError) {
            log.error("AI 调用失败 - HTTP {}, 响应体: {}", httpError.getStatusCode(),
                    httpError.getResponseBodyAsString());
        } else {
            log.error("AI 调用失败，返回降级响应", e);
        }
    }

    /**
     * 降级响应流：先发 degraded 事件告知前端本条不是模型生成，
     * 有待引用来源时随后发 sources 事件（供前端标注"原文摘录"并展示出处），
     * 最后按小块输出兜底文案（与正常回复一致的分块，避免超长 JSON 事件被网络分片截断）。
     */
    private Flux<ServerSentEvent<String>> degradedStream(String mockText,
                                                         List<Map<String, Object>> sources) {
        List<ServerSentEvent<String>> events = new ArrayList<>();
        events.add(sse(toJson(Map.of("degraded", true))));
        if (sources != null && !sources.isEmpty()) {
            events.add(sse(toJson(Map.of("sources", sources))));
        }
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
     * @param degraded  是否为降级回复；降级回复若摘录了知识库原文，仍会保存来源，保证回放一致
     */
    private void saveAssistantMessage(Long sessionId, String content,
                                      List<Map<String, Object>> sources, boolean degraded) {
        if (content == null || content.isEmpty()) {
            return;
        }
        ChatMessage assistantMsg = new ChatMessage();
        assistantMsg.setSessionId(sessionId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(content);
        assistantMsg.setSources(sources == null || sources.isEmpty() ? null : toJson(sources));
        assistantMsg.setDegraded(degraded);
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
