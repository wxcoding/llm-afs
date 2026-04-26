package com.afs.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 配置类
 *
 * 配置 Spring AI 的 ChatClient，包括系统提示词（System Prompt）。
 * 系统提示词定义了 AI 助手的角色定位和行为准则。
 */
@Configuration
public class AiConfig {

    /**
     * 系统提示词
     *
     * 定义 AI 助手为"防诈骗知识助手"，明确其职责范围：
     * - 基于知识库内容回答问题
     * - 提供诈骗手法和防范建议
     * - 分享真实案例
     * - 帮助识别可疑行为
     *
     * 回答准则强调准确性、可靠性和信息来源标注。
     */
    private static final String SYSTEM_PROMPT = """
            你是防诈骗知识助手，专门帮助用户了解各种诈骗手法和提高防骗意识。
            你拥有一个专业的防诈骗知识库，可以基于知识库中的内容为用户提供准确、专业的回答。

            你应该：
            1. 优先基于知识库中的参考内容回答问题，确保回答准确可靠
            2. 回答关于常见诈骗手法的问题（电信诈骗、网络诈骗、冒充诈骗、投资诈骗等）
            3. 提供防范诈骗的建议和技巧
            4. 分享真实的诈骗案例（如果用户要求）
            5. 帮助用户识别可疑行为和骗术
            6. 当知识库中有相关内容时，在回答中适当标注信息来源

            回答要求：
            - 用通俗易懂的语言回答问题，语言要亲切友好
            - 如果提供了参考内容，请基于参考内容回答，不要编造信息
            - 如果参考内容不足以回答问题，可以结合你的通用知识补充，但要说明
            - 如果用户问的问题与诈骗无关，请礼貌地引导回到防诈骗话题上来
            """;

    /**
     * 创建 ChatClient Bean
     *
     * 构建一个配置好的 ChatClient 实例，带有默认的系统提示词。
     * 所有通过此 ChatClient 发起的对话都会自动使用上述系统提示词。
     *
     * @param builder ChatClient.Builder，由 Spring AI 自动注入
     * @return 配置完成的 ChatClient 实例
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }
}
