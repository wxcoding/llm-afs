package com.afs.module.knowledge.service;

import com.afs.module.knowledge.entity.KnowledgeTag;
import com.afs.module.knowledge.entity.KnowledgeTagRelation;
import com.afs.module.knowledge.mapper.KnowledgeAuditMapper;
import com.afs.module.knowledge.mapper.KnowledgeMapper;
import com.afs.module.knowledge.mapper.KnowledgeTagMapper;
import com.afs.module.knowledge.mapper.KnowledgeTagRelationMapper;
import com.afs.module.knowledge.mapper.KnowledgeVersionMapper;
import com.afs.module.rag.ingestion.RagIngestionService;
import com.afs.module.rag.search.RagSearchService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * KnowledgeService 标签管理单元测试
 *
 * 覆盖：创建标签（字段初始化）、查询全部标签、删除标签（级联删关联）、
 * 绑定标签（插关联 + 计数自增）、解绑标签（删关联 + 计数自减）、
 * 查询知识关联标签（有关联/无关联两个分支）
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTagTest {

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

    // 创建标签：初始化计数为 0 并落库
    @Test
    void createTag_initializesFieldsAndInserts() {
        KnowledgeTag tag = knowledgeService.createTag("高危", "red", "高危诈骗类型");

        ArgumentCaptor<KnowledgeTag> captor = ArgumentCaptor.forClass(KnowledgeTag.class);
        verify(tagMapper).insert(captor.capture());
        KnowledgeTag inserted = captor.getValue();
        assertEquals("高危", inserted.getName());
        assertEquals("red", inserted.getColor());
        assertEquals("高危诈骗类型", inserted.getDescription());
        assertEquals(0, inserted.getKnowledgeCount());
        assertNotNull(inserted.getCreateTime());
        assertSame(inserted, tag);
    }

    // 查询全部标签透传 mapper 结果
    @Test
    void getAllTags_returnsMapperList() {
        List<KnowledgeTag> expected = List.of(new KnowledgeTag());
        when(tagMapper.selectList(any())).thenReturn(expected);

        assertSame(expected, knowledgeService.getAllTags());
    }

    // 删除标签：先删关联关系再删标签本身
    @Test
    void deleteTag_deletesRelationsThenTag() {
        knowledgeService.deleteTag(5L);

        verify(tagRelationMapper).delete(any(QueryWrapper.class));
        verify(tagMapper).deleteById(5L);
    }

    // 绑定标签：插入关联记录并将标签计数加一
    @Test
    void addTagToKnowledge_insertsRelationAndIncrementsCount() {
        knowledgeService.addTagToKnowledge(10L, 5L);

        ArgumentCaptor<KnowledgeTagRelation> captor = ArgumentCaptor.forClass(KnowledgeTagRelation.class);
        verify(tagRelationMapper).insert(captor.capture());
        assertEquals(10L, captor.getValue().getKnowledgeId());
        assertEquals(5L, captor.getValue().getTagId());
        assertNotNull(captor.getValue().getCreateTime());
        // 通过 UpdateWrapper 的 setSql 实现计数自增
        verify(tagMapper).update(isNull(), any(UpdateWrapper.class));
    }

    // 解绑标签：删除关联记录并将标签计数减一
    @Test
    void removeTagFromKnowledge_deletesRelationAndDecrementsCount() {
        knowledgeService.removeTagFromKnowledge(10L, 5L);

        verify(tagRelationMapper).delete(any(QueryWrapper.class));
        verify(tagMapper).update(isNull(), any(UpdateWrapper.class));
    }

    // 查询知识关联标签：有关联时按标签 ID 批量查询
    @Test
    void getTagsForKnowledge_withRelations_returnsTags() {
        KnowledgeTagRelation r1 = new KnowledgeTagRelation();
        r1.setTagId(1L);
        KnowledgeTagRelation r2 = new KnowledgeTagRelation();
        r2.setTagId(2L);
        when(tagRelationMapper.selectList(any())).thenReturn(List.of(r1, r2));
        List<KnowledgeTag> tags = List.of(new KnowledgeTag(), new KnowledgeTag());
        when(tagMapper.selectList(any())).thenReturn(tags);

        assertSame(tags, knowledgeService.getTagsForKnowledge(10L));
        verify(tagMapper).selectList(any());
    }

    // 查询知识关联标签：无关联时返回空列表且不再查询标签表
    @Test
    void getTagsForKnowledge_noRelations_returnsEmptyAndSkipsTagQuery() {
        when(tagRelationMapper.selectList(any())).thenReturn(List.of());

        List<KnowledgeTag> result = knowledgeService.getTagsForKnowledge(10L);

        assertTrue(result.isEmpty());
        verify(tagMapper, never()).selectList(any());
    }
}
