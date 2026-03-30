package com.afs.service;

import com.afs.entity.ChatMessage;
import com.afs.entity.ChatSession;
import com.afs.mapper.ChatMessageMapper;
import com.afs.mapper.ChatSessionMapper;
import org.springframework.ai.chat.client.ChatClient;
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

@Service
public class ChatService {

    @Autowired
    private ChatSessionMapper sessionMapper;

    @Autowired
    private ChatMessageMapper messageMapper;

    @Autowired
    private ChatClient chatClient;

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

        List<Message> messages = buildMessages(sessionId);
        
        String aiResponse = callAI(messages, content);

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

    public void sendMessageStream(Long userId, Long sessionId, String content, PrintWriter writer) throws Exception {
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

        // 保存用户消息
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(content);
        userMsg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(userMsg);

        // 构建消息历史
        List<Message> messages = buildMessages(sessionId);
        
        // 发送会话ID
        Map<String, Object> sessionInfo = new HashMap<>();
        sessionInfo.put("sessionId", sessionId);
        writer.write("data: " + new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(sessionInfo) + "\n\n");
        writer.flush();
        
        // 调用AI并流式输出
        String aiResponse = callAIStream(messages, content, writer);

        // 保存助手消息
        ChatMessage assistantMsg = new ChatMessage();
        assistantMsg.setSessionId(sessionId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(aiResponse);
        assistantMsg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(assistantMsg);
    }

    private String callAIStream(List<Message> historyMessages, String userContent, PrintWriter writer) throws Exception {
        try {
            // 调用大模型的同步API获取完整响应
            ChatClient.ChatClientRequestSpec request = chatClient.prompt()
                    .messages(historyMessages)
                    .user(userContent);
            
            String fullResponse = request.call().content();
            
            // 模拟流式输出，将完整响应分块发送
            StringBuilder responseBuilder = new StringBuilder();
            int chunkSize = 50; // 每50个字符为一个块
            
            for (int i = 0; i < fullResponse.length(); i += chunkSize) {
                int end = Math.min(i + chunkSize, fullResponse.length());
                String chunk = fullResponse.substring(i, end);
                responseBuilder.append(chunk);
                
                // 发送流式数据
                Map<String, Object> data = new HashMap<>();
                data.put("content", chunk);
                writer.write("data: " + new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(data) + "\n\n");
                writer.flush();
                
                // 模拟网络延迟，使流式效果更明显
                Thread.sleep(100);
            }
            
            return responseBuilder.toString();
        } catch (Exception e) {
            // 发生错误时使用模拟响应
            String mockResponse = getMockResponse(userContent);
            Map<String, Object> data = new HashMap<>();
            data.put("content", mockResponse);
            writer.write("data: " + new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(data) + "\n\n");
            writer.flush();
            return mockResponse;
        }
    }

    private List<Message> buildMessages(Long sessionId) {
        List<Message> messages = new ArrayList<>();

        List<ChatMessage> history = messageMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatMessage>()
                        .eq("session_id", sessionId)
                        .orderByAsc("create_time")
        );

        for (ChatMessage msg : history) {
            if (msg.getContent() != null) {
                if ("user".equals(msg.getRole())) {
                    messages.add(new UserMessage(msg.getContent()));
                } else if ("assistant".equals(msg.getRole())) {
                    messages.add(new org.springframework.ai.chat.messages.AssistantMessage(msg.getContent()));
                }
            }
        }
        return messages;
    }

    private String callAI(List<Message> historyMessages, String userContent) {
        try {
            ChatClient.ChatClientRequestSpec request = chatClient.prompt()
                    .messages(historyMessages)
                    .user(userContent);

            return request.call().content();
        } catch (Exception e) {
            return getMockResponse(userContent);
        }
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
