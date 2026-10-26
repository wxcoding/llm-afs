package com.afs.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 *
 * 知识库/案例写入向量索引依赖外部 Embedding 接口，耗时秒级，
 * 通过专用线程池异步执行，避免阻塞管理端 HTTP 请求。
 * 队列满时回退调用方线程执行（CallerRunsPolicy），形成背压。
 * void 异步方法的异常统一由 uncaughtExceptionHandler 记录，避免静默丢失。
 */
@Slf4j
@EnableAsync
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    /** RAG 文档索引线程池：IO 密集（Embedding HTTP 调用），线程数不宜过大以防打满限流 */
    @Bean("ragIngestionExecutor")
    public Executor ragIngestionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("rag-ingest-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /** void 异步方法抛出的异常无法回传调用方，统一记录日志 */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                log.error("异步任务执行失败: {}.{}，参数: {}", method.getDeclaringClass().getSimpleName(),
                        method.getName(), params, ex);
    }
}
