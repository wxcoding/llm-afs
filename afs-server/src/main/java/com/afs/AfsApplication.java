package com.afs;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.afs.mapper")
public class AfsApplication {
    public static void main(String[] args) {
        SpringApplication.run(AfsApplication.class, args);
    }
}
