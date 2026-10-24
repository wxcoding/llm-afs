package com.afs.module.knowledge.service;

import com.afs.enums.KnowledgeStatus;
import com.afs.exception.BusinessException;
import com.afs.module.knowledge.entity.Knowledge;
import com.afs.module.knowledge.mapper.KnowledgeAuditMapper;
import com.afs.module.knowledge.mapper.KnowledgeMapper;
import com.afs.module.knowledge.mapper.KnowledgeTagMapper;
import com.afs.module.knowledge.mapper.KnowledgeTagRelationMapper;
import com.afs.module.knowledge.mapper.KnowledgeVersionMapper;
import com.afs.module.rag.ingestion.RagIngestionService;
import com.afs.module.rag.search.RagSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * KnowledgeService 基础 CRUD 与向量同步单元测试
 *
 * 覆盖：列表/分类/ID 查询、新增（默认状态与向量同步分支）、更新（不存在异常、
 * ACTIVE 重同步、非 ACTIVE 仅删向量）、删除、关键词搜索、语义搜索委托、
 * 默认知识初始化（空库插入/非空跳过）、全量向量同步（仅同步 ACTIVE 且有内容）
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeServiceCrudTest {

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
    private RagSearchService ragSearchService;

    @InjectMocks
    private KnowledgeService knowledgeService;

    // 查询全部知识列表
    @Test
    void getAllKnowledge_returnsMapperList() {
        List<Knowledge> expected = List.of(new Knowledge(), new Knowledge());
        when(knowledgeMapper.selectList(any())).thenReturn(expected);

        assertSame(expected, knowledgeService.getAllKnowledge());
        verify(knowledgeMapper).selectList(any());
    }

    // 按分类查询知识列表
    @Test
    void getKnowledgeByCategory_returnsMapperList() {
        List<Knowledge> expected = List.of(new Knowledge());
        when(knowledgeMapper.selectList(any())).thenReturn(expected);

        assertSame(expected, knowledgeService.getKnowledgeByCategory("防范技巧"));
    }

    // 按 ID 查询直接透传 mapper 结果
    @Test
    void getKnowledgeById_delegatesToMapper() {
        Knowledge knowledge = new Knowledge();
        when(knowledgeMapper.selectById(1L)).thenReturn(knowledge);

        assertSame(knowledge, knowledgeService.getKnowledgeById(1L));
    }

    // 新增时状态为空，默认置为 ACTIVE 且同步向量库
    @Test
    void addKnowledge_nullStatus_defaultsActiveAndSyncsVector() {
        Knowledge knowledge = new Knowledge();
        knowledge.setTitle("标题");
        knowledge.setCategory("分类");
        knowledge.setContent("内容");

        Knowledge result = knowledgeService.addKnowledge(knowledge);

        assertEquals(KnowledgeStatus.ACTIVE.getCode(), result.getStatus());
        assertNotNull(result.getCreateTime());
        verify(knowledgeMapper).insert(knowledge);
        // ACTIVE 且内容非空，需同步向量库（id 由数据库生成，mock 下为 null）
        verify(ragIngestionService).addKnowledgeDocument(isNull(), eq("标题"), eq("分类"), eq("内容"));
    }

    // 新增时状态为空字符串，同样按默认 ACTIVE 处理
    @Test
    void addKnowledge_emptyStatus_defaultsActive() {
        Knowledge knowledge = new Knowledge();
        knowledge.setStatus("");
        knowledge.setContent("内容");

        Knowledge result = knowledgeService.addKnowledge(knowledge);

        assertEquals(KnowledgeStatus.ACTIVE.getCode(), result.getStatus());
    }

    // 草稿状态新增不同步向量库
    @Test
    void addKnowledge_draftStatus_skipsVectorSync() {
        Knowledge knowledge = new Knowledge();
        knowledge.setStatus(KnowledgeStatus.DRAFT.getCode());
        knowledge.setContent("内容");

        knowledgeService.addKnowledge(knowledge);

        verify(ragIngestionService, never()).addKnowledgeDocument(any(), any(), any(), any());
    }

    // ACTIVE 状态但内容为空，不同步向量库
    @Test
    void addKnowledge_emptyContent_skipsVectorSync() {
        Knowledge knowledge = new Knowledge();
        knowledge.setContent("");

        knowledgeService.addKnowledge(knowledge);

        verify(ragIngestionService, never()).addKnowledgeDocument(any(), any(), any(), any());
    }

    // 更新不存在的知识抛出 404 业务异常
    @Test
    void updateKnowledge_notFound_throwsBusinessException() {
        when(knowledgeMapper.selectById(9L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> knowledgeService.updateKnowledge(9L, new Knowledge()));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("知识不存在", exception.getMessage());
        verifyNoInteractions(ragIngestionService);
    }

    // 更新 ACTIVE 知识：字段被覆盖且向量库先删后加
    @Test
    void updateKnowledge_active_resyncsVectorStore() {
        Knowledge existing = new Knowledge();
        existing.setStatus(KnowledgeStatus.ACTIVE.getCode());
        when(knowledgeMapper.selectById(1L)).thenReturn(existing);

        Knowledge update = new Knowledge();
        update.setTitle("新标题");
        update.setCategory("新分类");
        update.setContent("新内容");

        Knowledge result = knowledgeService.updateKnowledge(1L, update);

        assertEquals("新标题", result.getTitle());
        assertEquals("新分类", result.getCategory());
        assertEquals("新内容", result.getContent());
        verify(knowledgeMapper).updateById(existing);
        verify(ragIngestionService).deleteKnowledgeDocument(1L);
        verify(ragIngestionService).addKnowledgeDocument(null, "新标题", "新分类", "新内容");
    }

    // 更新非 ACTIVE 知识：仅删除旧向量，不再新增
    @Test
    void updateKnowledge_nonActive_onlyDeletesVector() {
        Knowledge existing = new Knowledge();
        existing.setStatus(KnowledgeStatus.PENDING_REVIEW.getCode());
        when(knowledgeMapper.selectById(1L)).thenReturn(existing);

        Knowledge update = new Knowledge();
        update.setContent("内容");

        knowledgeService.updateKnowledge(1L, update);

        verify(ragIngestionService).deleteKnowledgeDocument(1L);
        verify(ragIngestionService, never()).addKnowledgeDocument(any(), any(), any(), any());
    }

    // 删除知识：先删向量再删数据库记录
    @Test
    void deleteKnowledge_deletesVectorThenRow() {
        knowledgeService.deleteKnowledge(3L);

        verify(ragIngestionService).deleteKnowledgeDocument(3L);
        verify(knowledgeMapper).deleteById(3L);
    }

    // 关键词搜索透传 mapper 结果
    @Test
    void searchKnowledge_returnsMapperList() {
        List<Knowledge> expected = List.of(new Knowledge());
        when(knowledgeMapper.selectList(any())).thenReturn(expected);

        assertSame(expected, knowledgeService.searchKnowledge("诈骗"));
    }

    // 语义搜索委托给 RagSearchService
    @Test
    void semanticSearch_delegatesToRagSearchService() {
        List<Map<String, Object>> expected = List.of(Map.of("id", 1L));
        when(ragSearchService.searchWithMetadata("查询", 5)).thenReturn(expected);

        assertSame(expected, knowledgeService.semanticSearch("查询", 5));
    }

    // 空库初始化默认知识：插入 7 条预置内容
    @Test
    void initDefaultKnowledge_emptyDb_insertsPresetKnowledge() {
        when(knowledgeMapper.selectCount(any())).thenReturn(0L);

        knowledgeService.initDefaultKnowledge();

        verify(knowledgeMapper, times(7)).insert(any(Knowledge.class));
    }

    // 非空库跳过初始化
    @Test
    void initDefaultKnowledge_nonEmptyDb_skipsInsert() {
        when(knowledgeMapper.selectCount(any())).thenReturn(3L);

        knowledgeService.initDefaultKnowledge();

        verify(knowledgeMapper, never()).insert(any(Knowledge.class));
    }

    // 全量向量同步：清空索引后仅同步 ACTIVE 且内容非空的知识
    @Test
    void syncAllToVectorStore_clearsAndAddsOnlyActiveWithContent() {
        Knowledge active = new Knowledge();
        active.setStatus(KnowledgeStatus.ACTIVE.getCode());
        active.setTitle("t");
        active.setCategory("c");
        active.setContent("content");
        Knowledge draft = new Knowledge();
        draft.setStatus(KnowledgeStatus.DRAFT.getCode());
        draft.setContent("content");
        Knowledge activeNoContent = new Knowledge();
        activeNoContent.setStatus(KnowledgeStatus.ACTIVE.getCode());
        when(knowledgeMapper.selectList(any())).thenReturn(List.of(active, draft, activeNoContent));

        knowledgeService.syncAllToVectorStore();

        verify(ragIngestionService).clearKnowledgeIndex();
        verify(ragIngestionService, times(1)).addKnowledgeDocument(null, "t", "c", "content");
    }

    // 更新时将 DTO 字段拷贝到已存在的实体上（通过捕获参数验证）
    @Test
    void updateKnowledge_copiesFieldsOntoExistingEntity() {
        Knowledge existing = new Knowledge();
        existing.setStatus(KnowledgeStatus.DRAFT.getCode());
        when(knowledgeMapper.selectById(2L)).thenReturn(existing);

        Knowledge update = new Knowledge();
        update.setTitle("A");
        update.setCategory("B");
        update.setContent("C");
        knowledgeService.updateKnowledge(2L, update);

        ArgumentCaptor<Knowledge> captor = ArgumentCaptor.forClass(Knowledge.class);
        verify(knowledgeMapper).updateById(captor.capture());
        assertEquals("A", captor.getValue().getTitle());
        assertEquals("B", captor.getValue().getCategory());
        assertEquals("C", captor.getValue().getContent());
    }
}
