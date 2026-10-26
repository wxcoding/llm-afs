package com.afs.module.rag.ingestion;

import com.afs.enums.VectorStatus;
import com.afs.module.knowledge.entity.Knowledge;
import com.afs.module.knowledge.mapper.KnowledgeMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RAG 入库任务中断恢复。
 *
 * 入库任务在线程池的内存队列中排队，服务重启/发布会丢失未执行的任务
 * （业务记录已落库，但向量化没完成）。启动时扫描仍处于
 * PENDING / PROCESSING 状态的知识记录并重新派发，保证"重启不丢任务"。
 * @author guanwx
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class RagIngestionRecovery implements ApplicationRunner {

    /** 知识库记录 Mapper */
    private final KnowledgeMapper knowledgeMapper;
    /** 入库任务派发器，用于重新派发中断的入库任务 */
    private final RagIngestionDispatcher ragIngestionDispatcher;

    /**
     * 应用启动后扫描处于 PENDING / PROCESSING 状态的知识记录并重新派发入库。
     *
     * @param args 应用启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        List<Knowledge> interrupted = knowledgeMapper.selectList(new QueryWrapper<Knowledge>()
                .in("vector_status", VectorStatus.PENDING.getCode(), VectorStatus.PROCESSING.getCode()));
        if (interrupted.isEmpty()) {
            return;
        }
        log.info("[RAG入库] 发现 {} 条中断的入库任务，重新派发", interrupted.size());
        for (Knowledge knowledge : interrupted) {
            ragIngestionDispatcher.dispatchKnowledgeUpsert(knowledge.getId());
        }
    }
}
