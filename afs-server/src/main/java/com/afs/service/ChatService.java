package com.afs.service;

import com.afs.entity.ChatMessage;
import com.afs.entity.ChatSession;
import com.afs.mapper.ChatMessageMapper;
import com.afs.mapper.ChatSessionMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
public class ChatService {

    @Autowired
    private ChatSessionMapper sessionMapper;

    @Autowired
    private ChatMessageMapper messageMapper;

    @Value("${ai.dashscope.api-key}")
    private String apiKey;

    @Value("${ai.dashscope.model}")
    private String model;

    private static final String API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    private static final String SYSTEM_PROMPT = """ 
            你是防诈骗知识助手，专门帮助用户了解各种诈骗手法和提高防骗意识。
            你应该：
            1. 回答关于常见诈骗手法的问题（电信诈骗、网络诈骗、冒充诈骗、投资诈骗等）
            2. 提供防范诈骗的建议和技巧
            3. 分享真实的诈骗案例（如果用户要求）
            4. 帮助用户识别可疑行为和骗术
            
            请用通俗易懂的语言回答问题，语言要亲切友好。如果用户问的问题与诈骗无关，请礼貌地引导回到防诈骗话题上来。
            """;

    public Map<String, Object> sendMessage(Long userId, Long sessionId, String content) {
        ChatSession session;
        if (sessionId == null) {
            session = new ChatSession();
            session.setUserId(userId);
            session.setTitle(content.length() > 20 ? content.substring(0, 20) + "..." : content);
            session.setCreateTime(LocalDateTime.now());
            sessionMapper.insert(session);
            sessionId = session.getId();
        } else {
            session = sessionMapper.selectById(sessionId);
        }

        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(content);
        userMsg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(userMsg);

        List<Map<String, String>> messages = buildMessages(sessionId);
        messages.add(Map.of("role", "user", "content", content));

        String aiResponse = callAI(messages);

        ChatMessage assistantMsg = new ChatMessage();
        assistantMsg.setSessionId(sessionId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(aiResponse);
        assistantMsg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(assistantMsg);

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("session", session);
        result.put("response", aiResponse);
        result.put("userMessage", userMsg);
        result.put("assistantMessage", assistantMsg);
        return result;
    }

    private List<Map<String, String>> buildMessages(Long sessionId) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));

        List<ChatMessage> history = messageMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatMessage>()
                        .eq("session_id", sessionId)
                        .orderByAsc("create_time")
        );

        for (ChatMessage msg : history) {
            if (msg.getContent() != null) {
                messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
            }
        }
        return messages;
    }

    private String callAI(List<Map<String, String>> messages) {
        if (!StringUtils.hasText(apiKey) || "your-api-key-here".equals(apiKey)) {
            return getMockResponse(messages);
        }

        try {
            ObjectNode json = objectMapper.createObjectNode();
            json.put("model", model);

            ObjectNode input = objectMapper.createObjectNode();
            ArrayNode messagesArray = objectMapper.createArrayNode();
            for (Map<String, String> m : messages) {
                ObjectNode msg = objectMapper.createObjectNode();
                msg.put("role", m.get("role"));
                msg.put("content", m.get("content"));
                messagesArray.add(msg);
            }
            input.set("messages", messagesArray);

            ObjectNode parameters = objectMapper.createObjectNode();
            parameters.put("result_format", "message");
            parameters.put("temperature", 0.7);
            parameters.put("max_tokens", 1000);

            json.set("input", input);
            json.set("parameters", parameters);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            CountDownLatch latch = new CountDownLatch(1);
            final String[] responseBody = {null};
            final Exception[] error = {null};

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    error[0] = e;
                    latch.countDown();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.body() != null) {
                        responseBody[0] = response.body().string();
                    }
                    latch.countDown();
                }
            });

            latch.await(60, TimeUnit.SECONDS);

            if (error[0] != null) {
                return getMockResponse(messages);
            }

            if (responseBody[0] != null) {
                JsonNode result = objectMapper.readTree(responseBody[0]);
                if (result.has("output")) {
                    JsonNode output = result.get("output");
                    if (output.has("choices")) {
                        ArrayNode choices = (ArrayNode) output.get("choices");
                        if (choices.size() > 0) {
                            ObjectNode choice = (ObjectNode) choices.get(0);
                            if (choice.has("message")) {
                                JsonNode msg = choice.get("message");
                                return msg.get("content").asText();
                            }
                        }
                    }
                }
            }
            return getMockResponse(messages);

        } catch (Exception e) {
            return getMockResponse(messages);
        }
    }

    private String getMockResponse(List<Map<String, String>> messages) {
        String lastUserMsg = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).get("role"))) {
                lastUserMsg = messages.get(i).get("content");
                break;
            }
        }

        String lowerMsg = lastUserMsg.toLowerCase();

        if (lowerMsg.contains("电信诈骗") || lowerMsg.contains("诈骗") || lowerMsg.contains("防骗")) {
            return """
                防诈骗提示：
                
                1. **警惕陌生来电**：自称公检法、银行、快递等要求转账的都是诈骗！
                
                2. **不要轻信中奖信息**：天上不会掉馅饼，中奖先交钱都是骗局！
                
                3. **保护个人信息**：身份证号、银行卡号、验证码不要告诉陌生人！
                
                4. **核实对方身份**：遇到可疑情况，先挂断电话，再官方渠道核实！
                
                5. **不点击陌生链接**：短信、微信中的不明链接不要点！
                
                如遇到诈骗，请立即拨打110报警！如需进一步了解，请告诉我您想了解哪类诈骗手段。
                """;
        } else if (lowerMsg.contains("网络诈骗")) {
            return """
                网络诈骗常见手法：
                
                1. **虚假购物**：低价商品先付款后发货，然后消失
                2. **刷单返利**：前期小利，后期大额充值后无法提现
                3. **虚假投资**：高收益稳赚，典型的庞氏骗局
                4. **冒充客服**：假称退款、注销账户，诱导转账
                5. **杀猪盘**：网恋交友，诱导投资赌博
                
                防范建议：
                - 不轻信低价、中奖信息
                - 不随意转账给陌生人
                - 不下载不明软件
                - 不轻信高收益投资
                
                记住：只要让你转账给陌生人，就很可能是诈骗！
                """;
        } else if (lowerMsg.contains("冒充")) {
            return """
                冒充类诈骗常见手法：
                
                1. **冒充公检法**：称你涉嫌洗钱、非法出入境，要求转账到"安全账户"
                2. **冒充领导**：突然关心，然后让你帮忙转账
                3. **冒充熟人**：用新号码或社交软件借钱
                4. **冒充客服**：称商品有问题、航班取消要退款
                5. **冒充老师**：称孩子受伤需要医药费
                
                防范要点：
                - 公检法不会电话办案，更不会要求转账
                - 领导借钱要当面或视频核实
                - 熟人借钱务必电话确认
                
                如有疑问，请直接拨打官方客服电话核实！
                """;
        } else if (lowerMsg.contains("投资") || lowerMsg.contains("理财")) {
            return """
                投资理财诈骗警示：
                
                常见骗局：
                1. **荐股群**：老师免费推荐股票，先让你赚点钱，取得信任后诱导下载虚假APP
                2. **虚拟币**：虚构数字货币投资，实际是传销骗局
                3. **原始股**：声称公司即将上市，发行原始股
                4. **高息理财**：年化收益超过10%要警惕，超过20%基本是骗局
                5. **庞氏骗局**：用新钱还旧钱，终会崩盘
                
                识别要点：
                - 正规金融机构不会承诺保本高收益
                - 投资前查证资质，可到证监会官网查询
                - 不下载不明投资APP
                - 不向个人账户转账投资
                
                记住：收益与风险成正比，高收益必然高风险！
                """;
        } else {
            return """
                您好！我是防诈骗知识助手。
                
                我可以帮您了解：
                - 电信诈骗的常见手法和防范方法
                - 网络诈骗的各种套路
                - 冒充类诈骗的识别技巧
                - 投资理财诈骗的识别
                - 各类诈骗案例分享
                
                请告诉我您想了解什么内容？
                """;
        }
    }

    public List<ChatSession> getUserSessions(Long userId) {
        return sessionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatSession>()
                        .eq("user_id", userId)
                        .orderByDesc("create_time")
        );
    }

    public List<ChatMessage> getSessionMessages(Long sessionId) {
        return messageMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatMessage>()
                        .eq("session_id", sessionId)
                        .orderByAsc("create_time")
        );
    }
}
