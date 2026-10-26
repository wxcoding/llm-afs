package com.afs.module.knowledge.service;

import com.afs.enums.KnowledgeStatus;
import com.afs.module.knowledge.entity.Knowledge;
import com.afs.module.knowledge.entity.KnowledgeAudit;
import com.afs.module.knowledge.entity.KnowledgeVersion;
import com.afs.module.knowledge.mapper.KnowledgeAuditMapper;
import com.afs.module.knowledge.mapper.KnowledgeMapper;
import com.afs.module.knowledge.mapper.KnowledgeTagMapper;
import com.afs.module.knowledge.mapper.KnowledgeTagRelationMapper;
import com.afs.module.knowledge.mapper.KnowledgeVersionMapper;
import com.afs.module.rag.ingestion.RagIngestionDispatcher;
import com.afs.module.rag.ingestion.RagIngestionService;
import com.afs.module.rag.search.RagSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * KnowledgeService 版本控制与审核流程单元测试
 *
 * 版本控制覆盖：保存版本、版本历史查询、回滚（版本不存在返回 null、
 * ACTIVE/非 ACTIVE 知识回滚后均派发 MQ 入库任务，由消费者决定重建或清除索引）。
 * 审核流程覆盖：提交审核（新增/修改/知识不存在三分支）、待审核列表、
 * 审核通过（记录不存在、关联已有知识、新增知识、派发失败静默四分枝）、
 * 审核拒绝（记录不存在、知识置 REJECTED、知识缺失三分支）。
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeServiceVersionAuditTest {

    @Mock
    private KnowledgeMapper knowledgeMapper;
    @Mock
    private KnowledgeTagMapper tagMapper;
    @Mock
    private KnowledgeTagRelationMapper tagRelationMapper;
    @Mock
    private KnowledgeVersionMapper versionMapper;
    @Mock
    private KnowledgeAuditMapper auditMapper;
    @Mock
    private RagIngestionService ragIngestionService;
    @Mock
    private RagIngestionDispatcher ragIngestionDispatcher;
    @Mock
    private RagSearchService ragSearchService;

    @InjectMocks
    private KnowledgeService knowledgeService;

    // ==================== 版本控制 ====================

    // 保存版本：字段完整落库
    @Test
    void saveVersion_insertsWithAllFields() {
        KnowledgeVersion version = knowledgeService.saveVersion(1L, "标题", "内容", "修改摘要", 7L);

        ArgumentCaptor<KnowledgeVersion> captor = ArgumentCaptor.forClass(KnowledgeVersion.class);
        verify(versionMapper).insert(captor.capture());
        KnowledgeVersion inserted = captor.getValue();
        assertEquals(1L, inserted.getKnowledgeId());
        assertEquals("标题", inserted.getTitle());
        assertEquals("内容", inserted.getContent());
        assertEquals("修改摘要", inserted.getChangeSummary());
        assertEquals(7L, inserted.getOperatorId());
        assertNotNull(inserted.getCreateTime());
        assertSame(inserted, version);
    }

    // 查询版本历史透传 mapper 结果
    @Test
    void getVersionHistory_returnsMapperList() {
        List<KnowledgeVersion> expected = List.of(new KnowledgeVersion());
        when(versionMapper.selectList(any())).thenReturn(expected);

        assertSame(expected, knowledgeService.getVersionHistory(1L));
    }

    // 回滚：版本不存在返回 null，且不触碰知识与向量库
    @Test
    void rollbackToVersion_versionNotFound_returnsNull() {
        when(versionMapper.selectById(9L)).thenReturn(null);

        assertNull(knowledgeService.rollbackToVersion(9L));
        verifyNoInteractions(knowledgeMapper, ragIngestionService, ragIngestionDispatcher);
    }

    // 回滚：ACTIVE 知识先存当前版本再恢复目标内容，并派发入库任务重建索引
    @Test
    void rollbackToVersion_activeKnowledge_savesHistoryAndResyncs() {
        KnowledgeVersion target = new KnowledgeVersion();
        target.setKnowledgeId(1L);
        target.setTitle("旧标题");
        target.setContent("旧内容");
        when(versionMapper.selectById(100L)).thenReturn(target);

        Knowledge knowledge = new Knowledge();
        knowledge.setId(1L);
        knowledge.setTitle("当前标题");
        knowledge.setCategory("分类");
        knowledge.setContent("当前内容");
        knowledge.setStatus(KnowledgeStatus.ACTIVE.getCode());
        when(knowledgeMapper.selectById(1L)).thenReturn(knowledge);

        KnowledgeVersion result = knowledgeService.rollbackToVersion(100L);

        assertSame(target, result);
        // 回滚前将当前状态保存为历史版本
        verify(versionMapper).insert(any(KnowledgeVersion.class));
        // 知识内容恢复到目标版本
        ArgumentCaptor<Knowledge> captor = ArgumentCaptor.forClass(Knowledge.class);
        verify(knowledgeMapper).updateById(captor.capture());
        assertEquals("旧标题", captor.getValue().getTitle());
        assertEquals("旧内容", captor.getValue().getContent());
        // ACTIVE 状态派发 MQ 入库任务（先删后建由消费者完成）
        verify(ragIngestionDispatcher).dispatchKnowledgeUpsert(1L);
    }

    // 回滚：非 ACTIVE 知识恢复内容后同样派发任务，索引清除由消费者负责
    @Test
    void rollbackToVersion_nonActiveKnowledge_dispatchesUpsert() {
        KnowledgeVersion target = new KnowledgeVersion();
        target.setKnowledgeId(1L);
        target.setTitle("旧标题");
        target.setContent("旧内容");
        when(versionMapper.selectById(100L)).thenReturn(target);

        Knowledge knowledge = new Knowledge();
        knowledge.setId(1L);
        knowledge.setStatus(KnowledgeStatus.PENDING_REVIEW.getCode());
        when(knowledgeMapper.selectById(1L)).thenReturn(knowledge);

        knowledgeService.rollbackToVersion(100L);

        // 非 ACTIVE 状态同样派发任务，索引清除由消费者负责
        verify(ragIngestionDispatcher).dispatchKnowledgeUpsert(1L);
        verify(ragIngestionService, never()).addKnowledgeDocument(any(), any(), any(), any());
    }

    // 回滚的历史记录摘要包含目标版本 ID
    @Test
    void rollbackToVersion_historySummaryMentionsVersionId() {
        KnowledgeVersion target = new KnowledgeVersion();
        target.setKnowledgeId(1L);
        when(versionMapper.selectById(100L)).thenReturn(target);
        when(knowledgeMapper.selectById(1L)).thenReturn(new Knowledge());

        knowledgeService.rollbackToVersion(100L);

        ArgumentCaptor<KnowledgeVersion> captor = ArgumentCaptor.forClass(KnowledgeVersion.class);
        verify(versionMapper).insert(captor.capture());
        assertEquals("回滚到版本 100", captor.getValue().getChangeSummary());
    }

    // ==================== 审核流程 ====================

    // 提交审核（新增）：创建 PENDING_REVIEW 知识并插入 pending 审核记录
    @Test
    void submitForAudit_newKnowledge_insertsPendingKnowledgeAndAudit() {
        KnowledgeAudit audit = knowledgeService.submitForAudit(null, "标题", "内容", "分类", 7L);

        // 新增知识，状态为待审核
        ArgumentCaptor<Knowledge> knowledgeCaptor = ArgumentCaptor.forClass(Knowledge.class);
        verify(knowledgeMapper).insert(knowledgeCaptor.capture());
        assertEquals(KnowledgeStatus.PENDING_REVIEW.getCode(), knowledgeCaptor.getValue().getStatus());

        // 审核记录状态为 pending
        ArgumentCaptor<KnowledgeAudit> auditCaptor = ArgumentCaptor.forClass(KnowledgeAudit.class);
        verify(auditMapper).insert(auditCaptor.capture());
        KnowledgeAudit inserted = auditCaptor.getValue();
        assertEquals("pending", inserted.getAuditStatus());
        assertEquals("标题", inserted.getTitle());
        assertEquals(7L, inserted.getSubmitUserId());
        assertNotNull(inserted.getSubmitTime());
        assertSame(inserted, audit);
    }

    // 提交审核（修改已有知识）：知识置为待审核并下架向量
    @Test
    void submitForAudit_existingKnowledge_setsPendingAndDeletesVector() {
        Knowledge existing = new Knowledge();
        existing.setStatus(KnowledgeStatus.ACTIVE.getCode());
        when(knowledgeMapper.selectById(3L)).thenReturn(existing);

        knowledgeService.submitForAudit(3L, "新标题", "新内容", "新分类", 7L);

        ArgumentCaptor<Knowledge> captor = ArgumentCaptor.forClass(Knowledge.class);
        verify(knowledgeMapper).updateById(captor.capture());
        assertEquals(KnowledgeStatus.PENDING_REVIEW.getCode(), captor.getValue().getStatus());
        verify(ragIngestionService).deleteKnowledgeDocument(3L);
        verify(auditMapper).insert(any(KnowledgeAudit.class));
    }

    // 提交审核：knowledgeId 对应知识不存在时跳过更新，仍创建审核记录
    @Test
    void submitForAudit_knowledgeMissing_stillCreatesAudit() {
        when(knowledgeMapper.selectById(3L)).thenReturn(null);

        knowledgeService.submitForAudit(3L, "标题", "内容", "分类", 7L);

        verify(knowledgeMapper, never()).updateById(any(Knowledge.class));
        ArgumentCaptor<KnowledgeAudit> captor = ArgumentCaptor.forClass(KnowledgeAudit.class);
        verify(auditMapper).insert(captor.capture());
        assertEquals(3L, captor.getValue().getKnowledgeId());
        verifyNoInteractions(ragIngestionService, ragIngestionDispatcher);
    }

    // 查询待审核列表透传 mapper 结果
    @Test
    void getPendingAudits_returnsMapperList() {
        List<KnowledgeAudit> expected = List.of(new KnowledgeAudit());
        when(auditMapper.selectList(any())).thenReturn(expected);

        assertSame(expected, knowledgeService.getPendingAudits());
    }

    // 审核通过：审核记录不存在返回 null
    @Test
    void approveAudit_notFound_returnsNull() {
        when(auditMapper.selectById(9L)).thenReturn(null);

        assertNull(knowledgeService.approveAudit(9L, 1L, "同意"));
        verify(auditMapper, never()).updateById(any(KnowledgeAudit.class));
        verifyNoInteractions(knowledgeMapper, ragIngestionService, ragIngestionDispatcher);
    }

    // 审核通过（关联已有知识）：审核记录置 approved，知识更新为 ACTIVE 并同步向量库
    @Test
    void approveAudit_withKnowledgeId_updatesKnowledgeAndSyncsVector() {
        KnowledgeAudit audit = new KnowledgeAudit();
        audit.setKnowledgeId(5L);
        audit.setTitle("标题");
        audit.setContent("内容");
        audit.setCategory("分类");
        when(auditMapper.selectById(1L)).thenReturn(audit);

        KnowledgeAudit result = knowledgeService.approveAudit(1L, 8L, "同意");

        assertSame(audit, result);
        assertEquals("approved", result.getAuditStatus());
        assertEquals(8L, result.getAuditUserId());
        assertEquals("同意", result.getAuditComment());
        assertNotNull(result.getAuditTime());
        // 仅更新一次审核记录
        verify(auditMapper, times(1)).updateById(audit);
        // 已有知识更新为已发布
        ArgumentCaptor<Knowledge> captor = ArgumentCaptor.forClass(Knowledge.class);
        verify(knowledgeMapper).updateById(captor.capture());
        assertEquals(5L, captor.getValue().getId());
        assertEquals(KnowledgeStatus.ACTIVE.getCode(), captor.getValue().getStatus());
        // 派发 MQ 入库任务（先删后建由消费者完成）
        verify(ragIngestionDispatcher).dispatchKnowledgeUpsert(5L);
    }

    // 审核通过（新增知识场景）：创建正式知识记录并回填审核记录的 knowledgeId
    @Test
    void approveAudit_withoutKnowledgeId_createsKnowledgeAndBackfillsAudit() {
        KnowledgeAudit audit = new KnowledgeAudit();
        audit.setTitle("标题");
        audit.setContent("内容");
        audit.setCategory("分类");
        when(auditMapper.selectById(1L)).thenReturn(audit);

        knowledgeService.approveAudit(1L, 8L, null);

        // 新增正式知识记录，状态 ACTIVE
        ArgumentCaptor<Knowledge> captor = ArgumentCaptor.forClass(Knowledge.class);
        verify(knowledgeMapper).insert(captor.capture());
        assertEquals(KnowledgeStatus.ACTIVE.getCode(), captor.getValue().getStatus());
        // 审核记录更新两次：一次置状态，一次回填 knowledgeId
        verify(auditMapper, times(2)).updateById(audit);
        verify(knowledgeMapper, never()).updateById(any(Knowledge.class));
        // 派发 MQ 入库任务（id 由数据库生成，mock 下为 null）
        verify(ragIngestionDispatcher).dispatchKnowledgeUpsert(isNull());
    }

    // 审核通过：入库任务派发失败不影响审核结果（静默失败策略）
    @Test
    void approveAudit_vectorSyncFails_stillReturnsApproved() {
        KnowledgeAudit audit = new KnowledgeAudit();
        audit.setKnowledgeId(5L);
        audit.setContent("内容");
        when(auditMapper.selectById(1L)).thenReturn(audit);
        org.mockito.Mockito.doThrow(new RuntimeException("MQ 不可用"))
                .when(ragIngestionDispatcher).dispatchKnowledgeUpsert(5L);

        KnowledgeAudit result = knowledgeService.approveAudit(1L, 8L, "同意");

        assertEquals("approved", result.getAuditStatus());
    }

    // 审核拒绝：审核记录不存在返回 null
    @Test
    void rejectAudit_notFound_returnsNull() {
        when(auditMapper.selectById(9L)).thenReturn(null);

        assertNull(knowledgeService.rejectAudit(9L, 1L, "不合适"));
        verify(auditMapper, never()).updateById(any(KnowledgeAudit.class));
    }

    // 审核拒绝（关联已有知识）：记录置 rejected，知识置 REJECTED 并删除向量
    @Test
    void rejectAudit_withKnowledge_setsRejectedAndDeletesVector() {
        KnowledgeAudit audit = new KnowledgeAudit();
        audit.setKnowledgeId(5L);
        when(auditMapper.selectById(1L)).thenReturn(audit);
        Knowledge knowledge = new Knowledge();
        knowledge.setId(5L);
        knowledge.setStatus(KnowledgeStatus.PENDING_REVIEW.getCode());
        when(knowledgeMapper.selectById(5L)).thenReturn(knowledge);

        KnowledgeAudit result = knowledgeService.rejectAudit(1L, 8L, "内容不准确");

        assertSame(audit, result);
        assertEquals("rejected", result.getAuditStatus());
        assertEquals(8L, result.getAuditUserId());
        assertEquals("内容不准确", result.getAuditComment());
        assertNotNull(result.getAuditTime());
        // 知识状态置为已拒绝
        ArgumentCaptor<Knowledge> captor = ArgumentCaptor.forClass(Knowledge.class);
        verify(knowledgeMapper).updateById(captor.capture());
        assertEquals(KnowledgeStatus.REJECTED.getCode(), captor.getValue().getStatus());
        verify(ragIngestionService).deleteKnowledgeDocument(5L);
    }

    // 审核拒绝：知识已被删除时仅更新审核记录，不触碰向量库
    @Test
    void rejectAudit_knowledgeMissing_onlyUpdatesAudit() {
        KnowledgeAudit audit = new KnowledgeAudit();
        audit.setKnowledgeId(5L);
        when(auditMapper.selectById(1L)).thenReturn(audit);
        when(knowledgeMapper.selectById(5L)).thenReturn(null);

        knowledgeService.rejectAudit(1L, 8L, "拒绝");

        verify(knowledgeMapper, never()).updateById(any(Knowledge.class));
        verifyNoInteractions(ragIngestionService);
    }

    // 审核拒绝：新增场景的审核记录（无 knowledgeId）不更新知识表
    @Test
    void rejectAudit_withoutKnowledgeId_onlyUpdatesAudit() {
        KnowledgeAudit audit = new KnowledgeAudit();
        when(auditMapper.selectById(1L)).thenReturn(audit);

        knowledgeService.rejectAudit(1L, 8L, "拒绝");

        verify(auditMapper).updateById(audit);
        verifyNoInteractions(knowledgeMapper, ragIngestionService);
    }
}
