package com.afs.config;

import com.afs.security.AuthInterceptor;
import com.afs.security.RoleInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Web MVC 配置类
 *
 * 配置 CORS、登录鉴权拦截器、异步任务线程池等。
 */
@RequiredArgsConstructor
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final RoleInterceptor roleInterceptor;

    /**
     * CORS 允许的来源列表，逗号分隔；默认仅允许本地开发地址，
     * 部署时通过环境变量 APP_CORS_ALLOWED_ORIGINS 覆盖
     */
    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://127.0.0.1:3000,http://127.0.0.1:5173}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 登录鉴权：除注册/登录/文档/健康检查外全部需要 Bearer Token；
     * 角色校验在登录鉴权之后执行，按 @RequireRole 注解拦截管理员接口
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        String[] publicPaths = {
                "/api/user/register",
                "/api/user/login",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/actuator/health",
                "/error"
        };
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(publicPaths);
        registry.addInterceptor(roleInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(publicPaths);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/");
        registry.addResourceHandler("/v3/api-docs/**")
                .addResourceLocations("classpath:/META-INF/resources/v3/api-docs/");
    }

    /**
     * MVC 异步处理配置：SSE 流式接口（Flux 返回值）使用受控线程池，
     * 并设置 120s 异步超时，与 AI 流超时对齐，避免长流式响应被默认超时中断
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(asyncTaskExecutor());
        configurer.setDefaultTimeout(120_000L);
    }

    @Bean(name = "asyncTaskExecutor")
    public AsyncTaskExecutor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}