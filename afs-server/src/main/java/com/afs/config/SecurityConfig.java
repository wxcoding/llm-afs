package com.afs.config;

import org.springframework.context.annotation.Configuration;

/**
 * 安全配置类
 *
 * 提供安全相关的常量和异常类定义。
 */
@Configuration
public class SecurityConfig {

    public static final String SECRET_KEY = "afs-secret-key";

    public static class RateLimitException extends RuntimeException {
        public RateLimitException(String message) {
            super(message);
        }
    }
}