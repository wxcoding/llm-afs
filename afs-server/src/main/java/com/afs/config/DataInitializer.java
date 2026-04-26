package com.afs.config;

import com.afs.service.KnowledgeService;
import com.afs.service.ScamCaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 数据初始化配置
 *
 * 在应用启动时自动初始化默认数据，包括：
 * - 预设的诈骗案例数据
 * - 预设的知识库内容
 * - 向量库的同步
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    /**
     * 初始化默认数据
     *
     * 应用启动后自动执行，完成以下任务：
     * 1. 初始化预设的诈骗案例（如冒充公检法、刷单返利、杀猪盘等）
     * 2. 初始化预设的知识库内容
     * 3. 将所有数据同步到向量库，以便 AI 检索
     *
     * @param scamCaseService 诈骗案例服务
     * @param knowledgeService 知识库服务
     * @return CommandLineRunner，执行初始化任务
     */
    @Bean
    public CommandLineRunner initData(ScamCaseService scamCaseService, KnowledgeService knowledgeService) {
        return args -> {
            log.info("开始初始化默认数据...");
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
