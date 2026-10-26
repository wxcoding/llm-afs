package com.afs.module.system.service;

import com.afs.module.system.entity.SysDictItem;
import com.afs.module.system.entity.SysDictType;
import com.afs.module.system.mapper.SysDictItemMapper;
import com.afs.module.system.mapper.SysDictTypeMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 字典服务（Redis 缓存热点读，Redis 故障时 fail-open 直连数据库）
 *
 * @author guanwx
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysDictService {

    /** 字典缓存 Key 前缀 */
    private static final String DICT_KEY_PREFIX = "cache:dict:";
    /** 缓存过期时间 */
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    /** 字典类型数据访问接口 */
    private final SysDictTypeMapper dictTypeMapper;

    /** 字典项数据访问接口 */
    private final SysDictItemMapper dictItemMapper;

    /** Redis 操作模板 */
    private final StringRedisTemplate redisTemplate;

    /** JSON 序列化工具 */
    private final ObjectMapper objectMapper;

    /**
     * 启动时预热缓存
     */
    @PostConstruct
    public void initCache() {
        refreshAllCache();
        log.info("[字典系统] 缓存初始化完成");
    }

    /**
     * 刷新所有缓存
     */
    public void refreshAllCache() {
        List<SysDictType> types = dictTypeMapper.selectList(new QueryWrapper<SysDictType>().eq("status", 1));
        for (SysDictType type : types) {
            refreshDictCache(type.getDictCode());
        }
    }

    /**
     * 刷新指定字典缓存：失效后立即回源预热
     */
    public void refreshDictCache(String dictCode) {
        evict(DICT_KEY_PREFIX + dictCode);
        getDictItems(dictCode);
    }

    /**
     * 获取字典项列表（Redis 缓存，空集合也缓存防穿透）
     */
    public List<DictItemVO> getDictItems(String dictCode) {
        String key = DICT_KEY_PREFIX + dictCode;
        String cached = redisGet(key);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<DictItemVO>>() {});
            } catch (Exception e) {
                log.warn("[字典系统] 缓存反序列化失败，回源数据库: key={}", key);
            }
        }
        List<DictItemVO> items = loadDictItems(dictCode);
        redisSet(key, items);
        return items != null ? items : Collections.emptyList();
    }

    /**
     * 获取字典项名称
     */
    public String getDictItemName(String dictCode, String itemCode) {
        return getDictItems(dictCode).stream()
                .filter(item -> Objects.equals(item.getItemCode(), itemCode))
                .map(DictItemVO::getItemName)
                .findFirst()
                .orElse(itemCode);
    }

    /**
     * 批量获取多个字典
     */
    public Map<String, List<DictItemVO>> getBatchDicts(List<String> dictCodes) {
        Map<String, List<DictItemVO>> result = new HashMap<>();
        for (String dictCode : dictCodes) {
            result.put(dictCode, getDictItems(dictCode));
        }
        return result;
    }

    /**
     * 获取所有字典类型
     */
    public List<SysDictType> getAllDictTypes() {
        return dictTypeMapper.selectList(new QueryWrapper<SysDictType>().orderByAsc("id"));
    }

    /**
     * 创建字典类型
     */
    public SysDictType createDictType(SysDictType dictType) {
        dictType.setCreateTime(java.time.LocalDateTime.now());
        dictType.setUpdateTime(java.time.LocalDateTime.now());
        dictTypeMapper.insert(dictType);
        return dictType;
    }

    /**
     * 更新字典类型
     */
    public SysDictType updateDictType(SysDictType dictType) {
        dictType.setUpdateTime(java.time.LocalDateTime.now());
        dictTypeMapper.updateById(dictType);
        refreshDictCache(dictType.getDictCode());
        return dictType;
    }

    /**
     * 删除字典类型
     */
    public void deleteDictType(Long id) {
        SysDictType type = dictTypeMapper.selectById(id);
        if (type != null) {
            dictItemMapper.delete(new QueryWrapper<SysDictItem>().eq("dict_code", type.getDictCode()));
            dictTypeMapper.deleteById(id);
            evict(DICT_KEY_PREFIX + type.getDictCode());
        }
    }

    /**
     * 创建字典项
     */
    public SysDictItem createDictItem(SysDictItem dictItem) {
        dictItem.setCreateTime(java.time.LocalDateTime.now());
        dictItem.setUpdateTime(java.time.LocalDateTime.now());
        dictItemMapper.insert(dictItem);
        refreshDictCache(dictItem.getDictCode());
        return dictItem;
    }

    /**
     * 更新字典项
     */
    public SysDictItem updateDictItem(SysDictItem dictItem) {
        dictItem.setUpdateTime(java.time.LocalDateTime.now());
        dictItemMapper.updateById(dictItem);
        refreshDictCache(dictItem.getDictCode());
        return dictItem;
    }

    /**
     * 删除字典项
     */
    public void deleteDictItem(Long id) {
        SysDictItem item = dictItemMapper.selectById(id);
        if (item != null) {
            dictItemMapper.deleteById(id);
            refreshDictCache(item.getDictCode());
        }
    }

    /**
     * Redis 读取（fail-open：异常时返回 null，走回源）
     */
    private String redisGet(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("[字典系统] Redis 读取失败，回源数据库: key={}, 原因: {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * Redis 写入（fail-open：异常仅记录日志）
     */
    private void redisSet(String key, List<DictItemVO> items) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(items), CACHE_TTL);
        } catch (Exception e) {
            log.warn("[字典系统] Redis 写入失败: key={}, 原因: {}", key, e.getMessage());
        }
    }

    /**
     * 缓存失效（fail-open：异常仅记录日志）
     */
    private void evict(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("[字典系统] Redis 删除失败: key={}, 原因: {}", key, e.getMessage());
        }
    }

    /**
     * 从数据库加载字典项并转换为 VO（回源逻辑）
     */
    private List<DictItemVO> loadDictItems(String dictCode) {
        List<SysDictItem> items = dictItemMapper.selectList(
                new QueryWrapper<SysDictItem>()
                        .eq("dict_code", dictCode)
                        .eq("status", 1)
                        .orderByAsc("sort"));
        return items.stream().map(item -> {
            DictItemVO vo = new DictItemVO();
            vo.setItemCode(item.getItemCode());
            vo.setItemName(item.getItemName());
            vo.setItemValue(item.getItemValue());
            vo.setSort(item.getSort());
            vo.setRemark(item.getRemark());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 字典项VO
     */
    public static class DictItemVO {
        /** 字典项编码 */
        private String itemCode;
        /** 字典项名称 */
        private String itemName;
        /** 字典项值 */
        private String itemValue;
        /** 排序 */
        private Integer sort;
        /** 备注 */
        private String remark;

        public String getItemCode() { return itemCode; }
        public void setItemCode(String itemCode) { this.itemCode = itemCode; }
        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }
        public String getItemValue() { return itemValue; }
        public void setItemValue(String itemValue) { this.itemValue = itemValue; }
        public Integer getSort() { return sort; }
        public void setSort(Integer sort) { this.sort = sort; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }
}
