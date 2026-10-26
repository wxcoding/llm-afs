package com.afs.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate 配置类
 *
 * 提供带超时控制的 RestTemplate Bean，用于调用外部 HTTP API（如 ReRank 服务）。
 *
 * @author guanwx
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 配置外部 ReRank API 的连接和读取超时，避免请求无限期占用处理线程。
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(30))
                .build();
    }
}
