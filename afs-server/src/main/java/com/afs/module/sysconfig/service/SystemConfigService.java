package com.afs.module.sysconfig.service;

import com.afs.module.sysconfig.entity.SystemConfig;
import com.afs.module.sysconfig.mapper.SystemConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置服务类
 *
 * 负责系统参数配置的管理和初始化；getConfigValue 走 Redis 缓存（fail-open 直连数据库）
 *
 * @author guanwx
 */
@Slf4j
@Service
public class SystemConfigService extends ServiceImpl<SystemConfigMapper, SystemConfig> {

    /** 配置缓存的 Redis key 前缀 */
    private static final String CONFIG_KEY_PREFIX = "cache:sysconfig:";
    /** 配置缓存的过期时间 */
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    /** 内置默认配置项（配置键到默认值的映射） */
    private static final Map<String, String> DEFAULT_CONFIGS = new HashMap<>();
    static {
        DEFAULT_CONFIGS.put("system_prompt", "你是一个专业的防诈骗助手，请帮助用户识别和防范各种诈骗手段。");
        DEFAULT_CONFIGS.put("model_name", "gpt-3.5-turbo");
        DEFAULT_CONFIGS.put("max_tokens", "2000");
        DEFAULT_CONFIGS.put("temperature", "0.7");
        DEFAULT_CONFIGS.put("vector_sync_interval", "3600");
    }

    /** Redis 字符串模板，用于配置值缓存 */
    private final StringRedisTemplate redisTemplate;

    /**
     * 构造系统配置服务
     *
     * @param redisTemplate Redis 字符串模板，用于配置值缓存
     */
    public SystemConfigService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 初始化默认配置项：数据库中不存在的配置插入默认值，并清除对应缓存
     */
    public void initDefaultConfigs() {
        for (Map.Entry<String, String> entry : DEFAULT_CONFIGS.entrySet()) {
            SystemConfig existing = baseMapper.selectOne(
                    new QueryWrapper<SystemConfig>().eq("config_key", entry.getKey()));
            if (existing == null) {
                SystemConfig config = new SystemConfig();
                config.setConfigKey(entry.getKey());
                config.setConfigValue(entry.getValue());
                config.setConfigType("system");
                config.setIsEditable(true);
                config.setCreateTime(LocalDateTime.now());
                config.setUpdateTime(LocalDateTime.now());
                baseMapper.insert(config);
                // 防止 Redis 中残留旧值覆盖新初始化的默认配置
                evict(CONFIG_KEY_PREFIX + entry.getKey());
            }
        }
    }

    /**
     * 更新指定配置项的值并清除缓存；配置不存在或不可编辑时不做修改
     *
     * @param configKey   配置键
     * @param configValue 新配置值
     * @return 更新后的配置项；配置不存在时返回 null
     */
    public SystemConfig updateConfig(String configKey, String configValue) {
        SystemConfig config = baseMapper.selectOne(
                new QueryWrapper<SystemConfig>().eq("config_key", configKey));
        if (config != null && config.getIsEditable()) {
            config.setConfigValue(configValue);
            config.setUpdateTime(LocalDateTime.now());
            baseMapper.updateById(config);
            evict(CONFIG_KEY_PREFIX + configKey);
        }
        return config;
    }

    /**
     * 获取配置值：优先读 Redis 缓存，未命中回源数据库（兜底默认值）并回填缓存
     *
     * @param configKey 配置键
     * @return 配置值；不存在时返回 null
     */
    public String getConfigValue(String configKey) {
        String key = CONFIG_KEY_PREFIX + configKey;
        String cached = redisGet(key);
        if (cached != null) {
            return cached;
        }
        SystemConfig config = baseMapper.selectOne(
                new QueryWrapper<SystemConfig>().eq("config_key", configKey));
        String value = config != null ? config.getConfigValue() : DEFAULT_CONFIGS.get(configKey);
        if (value != null) {
            redisSet(key, value);
        }
        return value;
    }

    /**
     * Redis 读取（fail-open：异常时返回 null，走回源）
     */
    private String redisGet(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("[系统配置] Redis 读取失败，回源数据库: key={}, 原因: {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * Redis 写入（fail-open：异常仅记录日志）
     */
    private void redisSet(String key, String value) {
        try {
            redisTemplate.opsForValue().set(key, value, CACHE_TTL);
        } catch (Exception e) {
            log.warn("[系统配置] Redis 写入失败: key={}, 原因: {}", key, e.getMessage());
        }
    }

    /**
     * 缓存失效（fail-open：异常仅记录日志）
     */
    private void evict(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("[系统配置] Redis 删除失败: key={}, 原因: {}", key, e.getMessage());
        }
    }

    /**
     * 获取所有配置项
     *
     * @return 全部系统配置列表
     */
    public List<SystemConfig> getAllConfigs() {
        return baseMapper.selectList(new QueryWrapper<>());
    }

    /**
     * 按配置类型获取配置项
     *
     * @param configType 配置类型
     * @return 配置键到配置值的映射
     */
    public Map<String, String> getConfigsByType(String configType) {
        List<SystemConfig> configs = baseMapper.selectList(
                new QueryWrapper<SystemConfig>().eq("config_type", configType));
        Map<String, String> result = new HashMap<>();
        for (SystemConfig config : configs) {
            result.put(config.getConfigKey(), config.getConfigValue());
        }
        return result;
    }
}
