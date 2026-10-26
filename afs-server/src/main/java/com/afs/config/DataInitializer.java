package com.afs.config;

import com.afs.module.sysconfig.service.SystemConfigService;
import com.afs.module.knowledge.service.KnowledgeService;
import com.afs.module.scamcase.service.ScamCaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 数据初始化配置
 *
 * 应用启动后初始化默认系统配置、诈骗案例与知识库数据，
 * 并同步到向量库；向量库同步失败仅记录警告，不影响应用启动。
 *
 * @author guanwx
 */
@Slf4j
@Configuration
public class DataInitializer {

    /**
     * 启动时初始化默认数据并同步向量库
     *
     * @param scamCaseService 诈骗案例服务
     * @param knowledgeService 知识库服务
     * @param systemConfigService 系统配置服务
     * @return 初始化执行器
     */
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
