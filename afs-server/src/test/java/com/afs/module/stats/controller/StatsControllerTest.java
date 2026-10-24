package com.afs.module.stats.controller;

import com.afs.module.stats.service.StatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * StatsController 单元测试
 *
 * 基于 MockMvc standalone 模式验证统计接口返回结构
 * 以及统计数据的正确透传。
 */
class StatsControllerTest {

    private MockMvc mockMvc;

    private StatsService statsService;

    @BeforeEach
    void setUp() {
        statsService = mock(StatsService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new StatsController(statsService)).build();
    }

    // 统计接口返回各模块计数
    @Test
    void getStatsReturnsAllCounts() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        stats.put("knowledgeCount", 10L);
        stats.put("caseCount", 20L);
        stats.put("sessionCount", 30L);
        stats.put("messageCount", 40L);
        stats.put("userCount", 50L);
        when(statsService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.knowledgeCount").value(10))
                .andExpect(jsonPath("$.data.caseCount").value(20))
                .andExpect(jsonPath("$.data.sessionCount").value(30))
                .andExpect(jsonPath("$.data.messageCount").value(40))
                .andExpect(jsonPath("$.data.userCount").value(50));
        verify(statsService).getStats();
    }
}
