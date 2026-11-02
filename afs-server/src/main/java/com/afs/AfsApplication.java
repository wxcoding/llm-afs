package com.afs;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author guanwx
 */
@SpringBootApplication
@MapperScan("com.afs.module.**.mapper")
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
