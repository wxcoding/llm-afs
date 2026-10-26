package com.afs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 安全配置类
 *
 * 提供密码加密器等安全相关组件。
 *
 * @author guanwx
 */
@Configuration
public class SecurityConfig {

    /**
     * 提供 BCrypt 密码加密器，用于用户密码的加密与校验
     *
     * @return BCrypt 密码加密器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
