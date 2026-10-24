package com.afs.module.scamcase.controller;

import com.afs.exception.BusinessException;
import com.afs.exception.GlobalExceptionHandler;
import com.afs.module.scamcase.entity.ScamCase;
import com.afs.module.scamcase.service.ScamCaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ScamCaseController 单元测试
 *
 * 基于 MockMvc standalone 模式验证诈骗案例 REST 接口的正常路径，
 * 以及案例不存在时的业务异常（404）响应。
 */
class ScamCaseControllerTest {

    private MockMvc mockMvc;

    private ScamCaseService scamCaseService;

    @BeforeEach
    void setUp() {
        scamCaseService = mock(ScamCaseService.class);
        // 注册全局异常处理器以覆盖业务异常分支
        mockMvc = MockMvcBuilders.standaloneSetup(new ScamCaseController(scamCaseService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // 不传 type 时返回全部案例
    @Test
    void getCasesWithoutTypeReturnsAll() throws Exception {
        when(scamCaseService.getAllCases()).thenReturn(List.of(buildCase(1L, "案例A")));

        mockMvc.perform(get("/api/cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("案例A"));
        verify(scamCaseService).getAllCases();
        verify(scamCaseService, never()).getCasesByType(any());
    }

    // 传入 type 时按类型筛选
    @Test
    void getCasesWithTypeFiltersByType() throws Exception {
        when(scamCaseService.getCasesByType("电信诈骗")).thenReturn(List.of(buildCase(2L, "案例B")));

        mockMvc.perform(get("/api/cases").param("type", "电信诈骗"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("案例B"));
        verify(scamCaseService).getCasesByType("电信诈骗");
    }

    // 传入空字符串 type 时视为未筛选
    @Test
    void getCasesWithBlankTypeReturnsAll() throws Exception {
        when(scamCaseService.getAllCases()).thenReturn(List.of());

        mockMvc.perform(get("/api/cases").param("type", ""))
                .andExpect(status().isOk());
        verify(scamCaseService).getAllCases();
    }

    // 按 ID 查询案例详情
    @Test
    void getCaseByIdReturnsCase() throws Exception {
        when(scamCaseService.getCaseById(1L)).thenReturn(buildCase(1L, "案例A"));

        mockMvc.perform(get("/api/cases/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("案例A"));
    }

    // 新增案例成功
    @Test
    void addCaseReturnsSuccessMessage() throws Exception {
        when(scamCaseService.addCase(any(ScamCase.class))).thenReturn(buildCase(1L, "新案例"));

        mockMvc.perform(post("/api/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"新案例\",\"type\":\"电信诈骗\",\"content\":\"正文\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("添加成功"))
                .andExpect(jsonPath("$.data.title").value("新案例"));
    }

    // 更新案例成功
    @Test
    void updateCaseReturnsSuccessMessage() throws Exception {
        when(scamCaseService.updateCase(eq(1L), any(ScamCase.class))).thenReturn(buildCase(1L, "更新后"));

        mockMvc.perform(put("/api/cases/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"更新后\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("更新成功"))
                .andExpect(jsonPath("$.data.title").value("更新后"));
    }

    // 更新不存在的案例时返回 404 业务错误
    @Test
    void updateCaseReturns404WhenCaseMissing() throws Exception {
        when(scamCaseService.updateCase(eq(99L), any(ScamCase.class)))
                .thenThrow(BusinessException.notFound("案例不存在"));

        mockMvc.perform(put("/api/cases/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"更新\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("案例不存在"));
    }

    // 删除案例成功
    @Test
    void deleteCaseReturnsSuccessMessage() throws Exception {
        mockMvc.perform(delete("/api/cases/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("删除成功"));
        verify(scamCaseService).deleteCase(1L);
    }

    private ScamCase buildCase(Long id, String title) {
        ScamCase scamCase = new ScamCase();
        scamCase.setId(id);
        scamCase.setTitle(title);
        scamCase.setType("电信诈骗");
        return scamCase;
    }
}
