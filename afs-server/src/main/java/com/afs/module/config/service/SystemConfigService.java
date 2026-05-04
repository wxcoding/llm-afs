package com.afs.module.config.service;

import com.afs.module.config.entity.SystemConfig;
import com.afs.module.config.mapper.SystemConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置服务类
 * 
 * 负责系统参数配置的管理和初始化
 */
@Service
public class SystemConfigService extends ServiceImpl<SystemConfigMapper, SystemConfig> {

    @Autowired(required = false)
    private SystemConfigMapper configMapper;

    private static final Map<String, String> DEFAULT_CONFIGS = new HashMap<>();
    static {
        DEFAULT_CONFIGS.put("system_prompt", "你是一个专业的防诈骗助手，请帮助用户识别和防范各种诈骗手段。");
        DEFAULT_CONFIGS.put("model_name", "gpt-3.5-turbo");
        DEFAULT_CONFIGS.put("max_tokens", "2000");
        DEFAULT_CONFIGS.put("temperature", "0.7");
        DEFAULT_CONFIGS.put("vector_sync_interval", "3600");
    }

    public void initDefaultConfigs() {
        for (Map.Entry<String, String> entry : DEFAULT_CONFIGS.entrySet()) {
            SystemConfig existing = configMapper.selectOne(
                    new QueryWrapper<SystemConfig>().eq("config_key", entry.getKey()));
            if (existing == null) {
                SystemConfig config = new SystemConfig();
                config.setConfigKey(entry.getKey());
                config.setConfigValue(entry.getValue());
                config.setConfigType("system");
                config.setIsEditable(true);
                config.setCreateTime(LocalDateTime.now());
                config.setUpdateTime(LocalDateTime.now());
                configMapper.insert(config);
            }
        }
    }

    public SystemConfig updateConfig(String configKey, String configValue) {
        SystemConfig config = configMapper.selectOne(
                new QueryWrapper<SystemConfig>().eq("config_key", configKey));
        if (config != null && config.getIsEditable()) {
            config.setConfigValue(configValue);
            config.setUpdateTime(LocalDateTime.now());
            configMapper.updateById(config);
        }
        return config;
    }

    public String getConfigValue(String configKey) {
        SystemConfig config = configMapper.selectOne(
                new QueryWrapper<SystemConfig>().eq("config_key", configKey));
        return config != null ? config.getConfigValue() : DEFAULT_CONFIGS.get(configKey);
    }

    public List<SystemConfig> getAllConfigs() {
        return configMapper.selectList(new QueryWrapper<>());
    }

    public Map<String, String> getConfigsByType(String configType) {
        List<SystemConfig> configs = configMapper.selectList(
                new QueryWrapper<SystemConfig>().eq("config_type", configType));
        Map<String, String> result = new HashMap<>();
        for (SystemConfig config : configs) {
            result.put(config.getConfigKey(), config.getConfigValue());
        }
        return result;
    }
}
