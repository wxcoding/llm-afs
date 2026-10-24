package com.afs.module.sysconfig.service;

import com.afs.module.sysconfig.entity.SystemConfig;
import com.afs.module.sysconfig.mapper.SystemConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SystemConfigService 单元测试
 *
 * 覆盖系统配置的初始化、读取（含默认值回退）、更新（可编辑性校验）与按类型聚合。
 * 由于继承 MyBatis-Plus ServiceImpl，通过 ReflectionTestUtils 注入 mock 的 baseMapper。
 */
class SystemConfigServiceTest {

    private SystemConfigMapper configMapper;

    private SystemConfigService configService;

    @BeforeEach
    void setUp() {
        configMapper = mock(SystemConfigMapper.class);
        configService = new SystemConfigService();
        // ServiceImpl 的 baseMapper 由父类持有，需反射注入
        ReflectionTestUtils.setField(configService, "baseMapper", configMapper);
    }

    // 初始化默认配置：全部 key 均不存在时插入 5 条默认配置
    @Test
    void initDefaultConfigsInsertsAllWhenMissing() {
        when(configMapper.selectOne(any())).thenReturn(null);

        configService.initDefaultConfigs();

        verify(configMapper, times(5)).insert(any(SystemConfig.class));
    }

    // 初始化默认配置：配置已存在时不重复插入
    @Test
    void initDefaultConfigsSkipsExistingKeys() {
        when(configMapper.selectOne(any())).thenReturn(buildConfig("system_prompt", "已有值", true));

        configService.initDefaultConfigs();

        verify(configMapper, never()).insert(any(SystemConfig.class));
    }

    // 更新配置：配置存在且可编辑时更新成功
    @Test
    void updateConfigUpdatesEditableConfig() {
        SystemConfig existing = buildConfig("model_name", "gpt-3.5-turbo", true);
        when(configMapper.selectOne(any())).thenReturn(existing);

        SystemConfig result = configService.updateConfig("model_name", "gpt-4");

        assertEquals("gpt-4", result.getConfigValue());
        assertNotNull(result.getUpdateTime());
        verify(configMapper).updateById(existing);
    }

    // 更新配置：配置不存在时返回 null 且不执行更新
    @Test
    void updateConfigReturnsNullWhenConfigMissing() {
        when(configMapper.selectOne(any())).thenReturn(null);

        SystemConfig result = configService.updateConfig("unknown_key", "value");

        assertNull(result);
        verify(configMapper, never()).updateById(any());
    }

    // 更新配置：配置不可编辑时保持原值且不执行更新
    @Test
    void updateConfigSkipsNonEditableConfig() {
        SystemConfig existing = buildConfig("locked_key", "原值", false);
        when(configMapper.selectOne(any())).thenReturn(existing);

        SystemConfig result = configService.updateConfig("locked_key", "新值");

        assertEquals("原值", result.getConfigValue());
        verify(configMapper, never()).updateById(any());
    }

    // 获取配置值：数据库存在时优先返回数据库值
    @Test
    void getConfigValuePrefersDatabaseValue() {
        when(configMapper.selectOne(any())).thenReturn(buildConfig("max_tokens", "4096", true));

        assertEquals("4096", configService.getConfigValue("max_tokens"));
    }

    // 获取配置值：数据库不存在时回退到内置默认值
    @Test
    void getConfigValueFallsBackToDefault() {
        when(configMapper.selectOne(any())).thenReturn(null);

        assertEquals("gpt-3.5-turbo", configService.getConfigValue("model_name"));
    }

    // 获取配置值：未知 key 且无默认值时返回 null
    @Test
    void getConfigValueReturnsNullForUnknownKey() {
        when(configMapper.selectOne(any())).thenReturn(null);

        assertNull(configService.getConfigValue("no_such_key"));
    }

    // 获取全部配置
    @Test
    void getAllConfigsReturnsMapperResult() {
        List<SystemConfig> configs = List.of(
                buildConfig("k1", "v1", true), buildConfig("k2", "v2", false));
        when(configMapper.selectList(any())).thenReturn(configs);

        assertEquals(2, configService.getAllConfigs().size());
    }

    // 按类型聚合配置为 key-value Map
    @Test
    void getConfigsByTypeAggregatesToMap() {
        List<SystemConfig> configs = List.of(
                buildConfig("model_name", "gpt-4", true),
                buildConfig("temperature", "0.5", true));
        when(configMapper.selectList(any())).thenReturn(configs);

        Map<String, String> result = configService.getConfigsByType("llm");

        assertEquals(2, result.size());
        assertEquals("gpt-4", result.get("model_name"));
        assertEquals("0.5", result.get("temperature"));
    }

    private SystemConfig buildConfig(String key, String value, boolean editable) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setIsEditable(editable);
        return config;
    }
}
