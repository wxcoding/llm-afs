package com.afs.module.rag.ingestion;

import com.afs.enums.VectorStatus;
import com.afs.module.knowledge.entity.Knowledge;
import com.afs.module.knowledge.mapper.KnowledgeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * RagIngestionDispatcher 单元测试
 *
 * 覆盖：知识任务派发前标记 PENDING、任务提交到线程池执行、
 * 案例任务不触碰 knowledge 状态字段。
 * 使用同步 Executor（Runnable::run），提交的任务立即在当前线程执行，便于断言。
 */
@ExtendWith(MockitoExtension.class)
class RagIngestionDispatcherTest {

    @Mock
    private RagIngestionProcessor ragIngestionProcessor;
    @Mock
    private KnowledgeMapper knowledgeMapper;

    private RagIngestionDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new RagIngestionDispatcher(ragIngestionProcessor, knowledgeMapper, Runnable::run);
    }

    // 派发知识任务：先将记录置为 PENDING，再提交线程池异步处理
    @Test
    void dispatchKnowledgeUpsert_marksPendingAndSubmitsToExecutor() {
        dispatcher.dispatchKnowledgeUpsert(1L);

        // 状态标记：仅更新 vector_status 为 PENDING
        ArgumentCaptor<Knowledge> captor = ArgumentCaptor.forClass(Knowledge.class);
        verify(knowledgeMapper).updateById(captor.capture());
        assertEquals(1L, captor.getValue().getId());
        assertEquals(VectorStatus.PENDING.getCode(), captor.getValue().getVectorStatus());

        // 任务已提交执行（同步 Executor 下处理器被直接调用）
        verify(ragIngestionProcessor).processKnowledge(1L);
    }

    // 派发案例任务：提交线程池处理，不触碰 knowledge 状态字段
    @Test
    void dispatchScamCaseUpsert_submitsWithoutTouchingStatus() {
        dispatcher.dispatchScamCaseUpsert(7L);

        verify(ragIngestionProcessor).processScamCase(7L);
        verifyNoInteractions(knowledgeMapper);
    }
}
