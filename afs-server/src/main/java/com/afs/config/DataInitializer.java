package com.afs.config;

import com.afs.module.config.service.SystemConfigService;
import com.afs.module.knowledge.service.KnowledgeService;
import com.afs.module.scamcase.service.ScamCaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initData(ScamCaseService scamCaseService, KnowledgeService knowledgeService,
                                      SystemConfigService systemConfigService) {
        return args -> {
            log.info("开始初始化默认数据...");
            systemConfigService.initDefaultConfigs();
            scamCaseService.initDefaultCases();
            knowledgeService.initDefaultKnowledge();
            log.info("默认数据初始化完成，开始同步向量库...");
            try {
                knowledgeService.syncAllToVectorStore();
                scamCaseService.syncAllToVectorStore();
                log.info("向量库同步完成");
            } catch (Exception e) {
                log.warn("向量库同步失败，请检查Embedding模型配置: {}", e.getMessage());
            }
        };
    }
}
