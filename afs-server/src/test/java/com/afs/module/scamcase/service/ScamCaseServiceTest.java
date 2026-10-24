package com.afs.module.scamcase.service;

import com.afs.exception.BusinessException;
import com.afs.module.rag.ingestion.RagIngestionService;
import com.afs.module.scamcase.entity.ScamCase;
import com.afs.module.scamcase.mapper.ScamCaseMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ScamCaseService 单元测试
 *
 * 覆盖诈骗案例的查询、新增、更新、删除、默认案例初始化与向量库同步逻辑，
 * 重点验证与 RAG 向量库的联动（增/改/删时的文档同步、空内容跳过同步）。
 */
@ExtendWith(MockitoExtension.class)
class ScamCaseServiceTest {

    @Mock
    private ScamCaseMapper scamCaseMapper;

    @Mock
    private RagIngestionService ragIngestionService;

    @InjectMocks
    private ScamCaseService scamCaseService;

    // 查询全部案例：按创建时间倒序返回
    @Test
    void getAllCasesReturnsMapperResult() {
        List<ScamCase> cases = List.of(buildCase(1L, "案例A"), buildCase(2L, "案例B"));
        when(scamCaseMapper.selectList(any())).thenReturn(cases);

        List<ScamCase> result = scamCaseService.getAllCases();

        assertEquals(2, result.size());
        assertEquals("案例A", result.get(0).getTitle());
    }

    // 按类型筛选案例
    @Test
    void getCasesByTypeDelegatesToMapper() {
        List<ScamCase> cases = List.of(buildCase(1L, "电信诈骗案例"));
        when(scamCaseMapper.selectList(any())).thenReturn(cases);

        List<ScamCase> result = scamCaseService.getCasesByType("电信诈骗");

        assertEquals(1, result.size());
    }

    // 按 ID 查询单个案例
    @Test
    void getCaseByIdReturnsEntity() {
        ScamCase scamCase = buildCase(5L, "案例X");
        when(scamCaseMapper.selectById(5L)).thenReturn(scamCase);

        assertEquals("案例X", scamCaseService.getCaseById(5L).getTitle());
    }

    // 新增案例：内容非空时同步向量库
    @Test
    void addCaseSyncsToVectorStoreWhenContentPresent() {
        ScamCase scamCase = buildCase(10L, "新案例");

        ScamCase result = scamCaseService.addCase(scamCase);

        assertNotNull(result.getCreateTime());
        verify(scamCaseMapper).insert(scamCase);
        // tips 为空时应以空字符串兜底
        verify(ragIngestionService).addScamCaseDocument(eq(10L), eq("新案例"), eq("电信诈骗"), eq("正文内容"), eq(""));
    }

    // 新增案例：内容为空时不同步向量库
    @Test
    void addCaseSkipsVectorSyncWhenContentEmpty() {
        ScamCase scamCase = buildCase(11L, "无内容案例");
        scamCase.setContent("");

        scamCaseService.addCase(scamCase);

        verify(scamCaseMapper).insert(scamCase);
        verifyNoInteractions(ragIngestionService);
    }

    // 更新案例：案例不存在时抛出业务异常
    @Test
    void updateCaseThrowsNotFoundWhenMissing() {
        when(scamCaseMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> scamCaseService.updateCase(99L, buildCase(99L, "更新")));
        assertEquals("案例不存在", ex.getMessage());
        verify(scamCaseMapper, never()).updateById(any());
        verifyNoInteractions(ragIngestionService);
    }

    // 更新案例：先删旧向量再按新内容重建
    @Test
    void updateCaseRebuildsVectorDocument() {
        ScamCase existing = buildCase(7L, "旧标题");
        when(scamCaseMapper.selectById(7L)).thenReturn(existing);
        ScamCase update = buildCase(null, "新标题");
        update.setTips("新提示");

        ScamCase result = scamCaseService.updateCase(7L, update);

        assertEquals("新标题", result.getTitle());
        assertEquals("新提示", result.getTips());
        verify(scamCaseMapper).updateById(existing);
        verify(ragIngestionService).deleteScamCaseDocument(7L);
        verify(ragIngestionService).addScamCaseDocument(7L, "新标题", "电信诈骗", "正文内容", "新提示");
    }

    // 更新案例：新内容为空时只删旧向量、不再新增
    @Test
    void updateCaseOnlyDeletesVectorWhenContentEmpty() {
        ScamCase existing = buildCase(8L, "旧标题");
        when(scamCaseMapper.selectById(8L)).thenReturn(existing);
        ScamCase update = buildCase(null, "新标题");
        update.setContent(null);

        scamCaseService.updateCase(8L, update);

        verify(ragIngestionService).deleteScamCaseDocument(8L);
        verify(ragIngestionService, never()).addScamCaseDocument(any(), any(), any(), any(), any());
    }

    // 删除案例：先删向量文档再删数据库记录
    @Test
    void deleteCaseRemovesVectorAndRow() {
        scamCaseService.deleteCase(3L);

        verify(ragIngestionService).deleteScamCaseDocument(3L);
        verify(scamCaseMapper).deleteById(3L);
    }

    // 初始化默认案例：表为空时插入全部 7 条内置案例
    @Test
    void initDefaultCasesInsertsWhenTableEmpty() {
        when(scamCaseMapper.selectCount(any())).thenReturn(0L);

        scamCaseService.initDefaultCases();

        verify(scamCaseMapper, times(7)).insert(any(ScamCase.class));
    }

    // 初始化默认案例：已有数据时不重复插入
    @Test
    void initDefaultCasesSkipsWhenTableNotEmpty() {
        when(scamCaseMapper.selectCount(any())).thenReturn(3L);

        scamCaseService.initDefaultCases();

        verify(scamCaseMapper, never()).insert(any(ScamCase.class));
    }

    // 全量同步向量库：先清空索引，空内容案例跳过
    @Test
    void syncAllToVectorStoreClearsIndexAndSkipsEmptyContent() {
        ScamCase withContent = buildCase(1L, "有内容");
        ScamCase withoutContent = buildCase(2L, "无内容");
        withoutContent.setContent(null);
        when(scamCaseMapper.selectList(any())).thenReturn(List.of(withContent, withoutContent));

        scamCaseService.syncAllToVectorStore();

        verify(ragIngestionService).clearScamCaseIndex();
        verify(ragIngestionService, times(1)).addScamCaseDocument(any(), any(), any(), any(), any());
    }

    private ScamCase buildCase(Long id, String title) {
        ScamCase scamCase = new ScamCase();
        scamCase.setId(id);
        scamCase.setTitle(title);
        scamCase.setType("电信诈骗");
        scamCase.setContent("正文内容");
        return scamCase;
    }
}
