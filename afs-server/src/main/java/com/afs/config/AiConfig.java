package com.afs.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    private static final String SYSTEM_PROMPT = """
            你是防诈骗知识助手，专门帮助用户了解各种诈骗手法和提高防骗意识。
            你应该：
            1. 回答关于常见诈骗手法的问题（电信诈骗、网络诈骗、冒充诈骗、投资诈骗等）
            2. 提供防范诈骗的建议和技巧
            3. 分享真实的诈骗案例（如果用户要求）
            4. 帮助用户识别可疑行为和骗术
            
            请用通俗易懂的语言回答问题，语言要亲切友好。如果用户问的问题与诈骗无关，请礼貌地引导回到防诈骗话题上来。
            """;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }
}
