package com.afs.module.sysconfig.controller;

import com.afs.exception.GlobalExceptionHandler;
import com.afs.module.sysconfig.entity.SystemConfig;
import com.afs.module.sysconfig.service.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SystemConfigController 单元测试
 *
 * 基于 MockMvc standalone 模式验证系统配置接口的查询、更新与初始化，
 * 包含缺少必需参数（configValue）时的 400 响应。
 */
class SystemConfigControllerTest {

    private MockMvc mockMvc;

    private SystemConfigService configService;

    @BeforeEach
    void setUp() {
        configService = mock(SystemConfigService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SystemConfigController(configService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // 获取全部配置
    @Test
    void getAllConfigsReturnsList() throws Exception {
        SystemConfig config = new SystemConfig();
        config.setConfigKey("model_name");
        config.setConfigValue("gpt-4");
        when(configService.getAllConfigs()).thenReturn(List.of(config));

        mockMvc.perform(get("/api/config/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].configKey").value("model_name"));
    }

    // 按配置键获取配置值
    // 注意：Result.success(String) 存在重载，String 类型的 data 实际落在 message 字段
    @Test
    void getConfigValueReturnsValue() throws Exception {
        when(configService.getConfigValue("model_name")).thenReturn("gpt-4");

        mockMvc.perform(get("/api/config/model_name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("gpt-4"));
    }

    // 按类型获取配置 Map
    @Test
    void getConfigsByTypeReturnsMap() throws Exception {
        when(configService.getConfigsByType("llm")).thenReturn(Map.of("model_name", "gpt-4"));

        mockMvc.perform(get("/api/config/type/llm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.model_name").value("gpt-4"));
    }

    // 更新配置成功
    @Test
    void updateConfigReturnsUpdatedConfig() throws Exception {
        SystemConfig updated = new SystemConfig();
        updated.setConfigKey("model_name");
        updated.setConfigValue("gpt-4");
        when(configService.updateConfig("model_name", "gpt-4")).thenReturn(updated);

        mockMvc.perform(put("/api/config/model_name").param("configValue", "gpt-4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configValue").value("gpt-4"));
    }

    // 更新配置：缺少必需的 configValue 参数时返回 400
    @Test
    void updateConfigReturns400WhenValueMissing() throws Exception {
        mockMvc.perform(put("/api/config/model_name"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("缺少必需参数: configValue"));
        verify(configService, never()).updateConfig(any(), any());
    }

    // 初始化默认配置
    @Test
    void initDefaultConfigsReturnsSuccess() throws Exception {
        mockMvc.perform(post("/api/config/init"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(configService).initDefaultConfigs();
    }
}
