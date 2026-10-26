package com.afs.module.rag.ingestion;

import com.afs.module.knowledge.entity.Knowledge;
import com.afs.module.knowledge.mapper.KnowledgeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * RagIngestionRecovery 单元测试
 *
 * 覆盖：启动时扫描到中断的入库任务则重新派发；无中断任务则不动作。
 */
@ExtendWith(MockitoExtension.class)
class RagIngestionRecoveryTest {

    @Mock
    private KnowledgeMapper knowledgeMapper;
    @Mock
    private RagIngestionDispatcher ragIngestionDispatcher;

    @InjectMocks
    private RagIngestionRecovery ragIngestionRecovery;

    // 无中断任务：不派发任何任务
    @Test
    void run_noInterruptedTasks_doesNothing() {
        when(knowledgeMapper.selectList(any())).thenReturn(List.of());

        ragIngestionRecovery.run(null);

        verifyNoInteractions(ragIngestionDispatcher);
    }

    // 存在 PENDING/PROCESSING 记录：逐条重新派发
    @Test
    void run_interruptedTasks_redispatchesEach() {
        Knowledge k1 = new Knowledge();
        k1.setId(1L);
        Knowledge k2 = new Knowledge();
        k2.setId(2L);
        when(knowledgeMapper.selectList(any())).thenReturn(List.of(k1, k2));

        ragIngestionRecovery.run(null);

        verify(ragIngestionDispatcher).dispatchKnowledgeUpsert(1L);
        verify(ragIngestionDispatcher).dispatchKnowledgeUpsert(2L);
    }
}
