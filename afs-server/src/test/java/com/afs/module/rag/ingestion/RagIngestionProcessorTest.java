package com.afs.module.rag.ingestion;

import com.afs.enums.KnowledgeStatus;
import com.afs.enums.VectorStatus;
import com.afs.module.knowledge.entity.Knowledge;
import com.afs.module.knowledge.mapper.KnowledgeMapper;
import com.afs.module.scamcase.entity.ScamCase;
import com.afs.module.scamcase.mapper.ScamCaseMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * RagIngestionProcessor 单元测试
 *
 * 覆盖：知识库文档处理的状态流转（PROCESSING -> SUCCESS/FAILED）、
 * 记录不存在跳过、非发布状态/空内容清除索引、处理异常静默处理；
 * 诈骗案例处理的新增/删除分支与异常静默。
 */
@ExtendWith(MockitoExtension.class)
class RagIngestionProcessorTest {

    @Mock
    private RagIngestionService ragIngestionService;
    @Mock
    private KnowledgeMapper knowledgeMapper;
    @Mock
    private ScamCaseMapper scamCaseMapper;

    @InjectMocks
    private RagIngestionProcessor ragIngestionProcessor;

    // 知识记录不存在：跳过处理，不更新状态、不触碰向量库
    @Test
    void processKnowledge_notFound_skips() {
        when(knowledgeMapper.selectById(9L)).thenReturn(null);

        ragIngestionProcessor.processKnowledge(9L);

        verify(knowledgeMapper, never()).updateById(any(Knowledge.class));
        verifyNoInteractions(ragIngestionService);
    }

    // ACTIVE 且内容非空：状态 PROCESSING -> SUCCESS，并构建索引
    @Test
    void processKnowledge_activeWithContent_indexesAndMarksSuccess() {
        Knowledge knowledge = buildKnowledge(1L, KnowledgeStatus.ACTIVE.getCode(), "内容");
        when(knowledgeMapper.selectById(1L)).thenReturn(knowledge);

        ragIngestionProcessor.processKnowledge(1L);

        verify(ragIngestionService).addKnowledgeDocument(1L, "标题", "分类", "内容");
        assertStatusTransitions(1L, VectorStatus.PROCESSING, VectorStatus.SUCCESS);
    }

    // 非发布状态：清除索引，状态仍流转为 SUCCESS
    @Test
    void processKnowledge_nonActive_deletesIndexAndMarksSuccess() {
        Knowledge knowledge = buildKnowledge(2L, KnowledgeStatus.PENDING_REVIEW.getCode(), "内容");
        when(knowledgeMapper.selectById(2L)).thenReturn(knowledge);

        ragIngestionProcessor.processKnowledge(2L);

        verify(ragIngestionService).deleteKnowledgeDocument(2L);
        verify(ragIngestionService, never()).addKnowledgeDocument(any(), any(), any(), any());
        assertStatusTransitions(2L, VectorStatus.PROCESSING, VectorStatus.SUCCESS);
    }

    // ACTIVE 但正文为空：清除索引
    @Test
    void processKnowledge_emptyContent_deletesIndex() {
        Knowledge knowledge = buildKnowledge(3L, KnowledgeStatus.ACTIVE.getCode(), "");
        when(knowledgeMapper.selectById(3L)).thenReturn(knowledge);

        ragIngestionProcessor.processKnowledge(3L);

        verify(ragIngestionService).deleteKnowledgeDocument(3L);
        assertStatusTransitions(3L, VectorStatus.PROCESSING, VectorStatus.SUCCESS);
    }

    // 向量化抛异常：状态流转为 FAILED，异常不向外抛出
    @Test
    void processKnowledge_ingestionFails_marksFailed() {
        Knowledge knowledge = buildKnowledge(4L, KnowledgeStatus.ACTIVE.getCode(), "内容");
        when(knowledgeMapper.selectById(4L)).thenReturn(knowledge);
        doThrow(new RuntimeException("Embedding 限流"))
                .when(ragIngestionService).addKnowledgeDocument(any(), any(), any(), any());

        ragIngestionProcessor.processKnowledge(4L);

        assertStatusTransitions(4L, VectorStatus.PROCESSING, VectorStatus.FAILED);
    }

    // 案例记录不存在：跳过处理
    @Test
    void processScamCase_notFound_skips() {
        when(scamCaseMapper.selectById(9L)).thenReturn(null);

        ragIngestionProcessor.processScamCase(9L);

        verifyNoInteractions(ragIngestionService);
    }

    // 案例内容非空：构建索引
    @Test
    void processScamCase_withContent_indexes() {
        ScamCase scamCase = new ScamCase();
        scamCase.setId(5L);
        scamCase.setTitle("案例");
        scamCase.setType("电信诈骗");
        scamCase.setContent("内容");
        scamCase.setTips("提示");
        when(scamCaseMapper.selectById(5L)).thenReturn(scamCase);

        ragIngestionProcessor.processScamCase(5L);

        verify(ragIngestionService).addScamCaseDocument(5L, "案例", "电信诈骗", "内容", "提示");
    }

    // 案例内容为空：清除索引
    @Test
    void processScamCase_emptyContent_deletesIndex() {
        ScamCase scamCase = new ScamCase();
        scamCase.setId(6L);
        scamCase.setContent(null);
        when(scamCaseMapper.selectById(6L)).thenReturn(scamCase);

        ragIngestionProcessor.processScamCase(6L);

        verify(ragIngestionService).deleteScamCaseDocument(6L);
        verify(ragIngestionService, never()).addScamCaseDocument(any(), any(), any(), any(), any());
    }

    // 案例向量化抛异常：静默处理不抛出
    @Test
    void processScamCase_ingestionFails_swallowed() {
        ScamCase scamCase = new ScamCase();
        scamCase.setId(7L);
        scamCase.setContent("内容");
        when(scamCaseMapper.selectById(7L)).thenReturn(scamCase);
        doThrow(new RuntimeException("向量库不可用"))
                .when(ragIngestionService).addScamCaseDocument(any(), any(), any(), any(), any());

        ragIngestionProcessor.processScamCase(7L);

        verify(ragIngestionService).addScamCaseDocument(any(), any(), any(), any(), any());
    }

    private Knowledge buildKnowledge(Long id, String status, String content) {
        Knowledge knowledge = new Knowledge();
        knowledge.setId(id);
        knowledge.setTitle("标题");
        knowledge.setCategory("分类");
        knowledge.setStatus(status);
        knowledge.setContent(content);
        return knowledge;
    }

    // 断言 vector_status 按期望顺序流转（updateById 只更新非空字段，捕获的实体仅含 id 与 vectorStatus）
    private void assertStatusTransitions(Long id, VectorStatus... expected) {
        ArgumentCaptor<Knowledge> captor = ArgumentCaptor.forClass(Knowledge.class);
        verify(knowledgeMapper, times(expected.length)).updateById(captor.capture());
        List<Knowledge> updates = captor.getAllValues();
        for (int i = 0; i < expected.length; i++) {
            assertEquals(id, updates.get(i).getId());
            assertEquals(expected[i].getCode(), updates.get(i).getVectorStatus());
        }
    }
}
