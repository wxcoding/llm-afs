package com.afs.module.rag.ingestion;

import com.afs.enums.VectorStatus;
import com.afs.module.knowledge.entity.Knowledge;
import com.afs.module.knowledge.mapper.KnowledgeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

/**
 * RAG 入库任务派发器。
 *
 * 文档切分与向量化是耗时操作（涉及外部 Embedding 接口），
 * 统一提交到独立线程池异步执行，不占用 Tomcat 请求线程；
 * 队列满时由 CallerRunsPolicy 提供背压。
 * 派发时将知识记录置为 PENDING，由 RagIngestionProcessor 流转后续状态。
 */
@Slf4j
@Component
public class RagIngestionDispatcher {

    private final RagIngestionProcessor ragIngestionProcessor;
    private final KnowledgeMapper knowledgeMapper;
    private final Executor ragIngestionExecutor;

    public RagIngestionDispatcher(RagIngestionProcessor ragIngestionProcessor,
                                  KnowledgeMapper knowledgeMapper,
                                  @Qualifier("ragIngestionExecutor") Executor ragIngestionExecutor) {
        this.ragIngestionProcessor = ragIngestionProcessor;
        this.knowledgeMapper = knowledgeMapper;
        this.ragIngestionExecutor = ragIngestionExecutor;
    }

    /**
     * 派发知识库文档入库任务（新增 / 内容变更 / 重建索引）。
     */
    public void dispatchKnowledgeUpsert(Long knowledgeId) {
        markPending(knowledgeId);
        ragIngestionExecutor.execute(() -> ragIngestionProcessor.processKnowledge(knowledgeId));
    }

    /**
     * 派发诈骗案例入库任务。
     */
    public void dispatchScamCaseUpsert(Long caseId) {
        ragIngestionExecutor.execute(() -> ragIngestionProcessor.processScamCase(caseId));
    }

    private void markPending(Long knowledgeId) {
        Knowledge update = new Knowledge();
        update.setId(knowledgeId);
        update.setVectorStatus(VectorStatus.PENDING.getCode());
        knowledgeMapper.updateById(update);
    }
}
