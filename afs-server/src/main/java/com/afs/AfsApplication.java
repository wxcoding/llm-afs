package com.afs;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AFS 应用启动类
 *
 * AFS（Anti-Fraud System）防诈骗系统是一个基于 RAG（检索增强生成）技术的
 * 智能防诈骗咨询平台后端服务。
 *
 * 主要功能：
 * - 用户注册、登录、个人信息管理
 * - 智能对话：基于知识库的防诈骗咨询
 * - 诈骗案例管理
 * - 知识库管理
 *
 * 技术栈：Spring Boot + MyBatis-Plus + PostgreSQL + Spring AI + pgvector
 */
@SpringBootApplication
@MapperScan("com.afs.mapper")
public class AfsApplication {

    /**
     * 应用入口，启动 Spring Boot 应用
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AfsApplication.class, args);
    }
}
