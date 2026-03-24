package com.afs.config;

import com.afs.service.KnowledgeService;
import com.afs.service.ScamCaseService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(ScamCaseService scamCaseService, KnowledgeService knowledgeService) {
        return args -> {
            scamCaseService.initDefaultCases();
            knowledgeService.initDefaultKnowledge();
        };
    }
}
