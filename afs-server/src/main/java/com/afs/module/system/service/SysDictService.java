package com.afs.module.system.service;

import com.afs.module.system.entity.SysDictItem;
import com.afs.module.system.entity.SysDictType;
import com.afs.module.system.mapper.SysDictItemMapper;
import com.afs.module.system.mapper.SysDictTypeMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 字典服务（含内存缓存）
 */
@Service
public class SysDictService {
    
    @Autowired
    private SysDictTypeMapper dictTypeMapper;
    
    @Autowired
    private SysDictItemMapper dictItemMapper;
    
    /**
     * 字典缓存（内存缓存）
     * key: dictCode
     * value: List<DictItemVO>
     */
    private final Map<String, List<DictItemVO>> dictCache = new ConcurrentHashMap<>();
    
    /**
     * 字典编码到名称映射缓存
     */
    private final Map<String, Map<String, String>> dictNameCache = new ConcurrentHashMap<>();
    
    /**
     * 启动时预热缓存
     */
    @PostConstruct
    public void initCache() {
        refreshAllCache();
        System.out.println("[字典系统] 缓存初始化完成");
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
     * 刷新指定字典缓存
     */
    public void refreshDictCache(String dictCode) {
        List<SysDictItem> items = dictItemMapper.selectList(
            new QueryWrapper<SysDictItem>()
                .eq("dict_code", dictCode)
                .eq("status", 1)
                .orderByAsc("sort")
        );
        
        List<DictItemVO> voList = items.stream().map(item -> {
            DictItemVO vo = new DictItemVO();
            vo.setItemCode(item.getItemCode());
            vo.setItemName(item.getItemName());
            vo.setItemValue(item.getItemValue());
            vo.setSort(item.getSort());
            vo.setRemark(item.getRemark());
            return vo;
        }).collect(Collectors.toList());
        
        dictCache.put(dictCode, voList);
        
        // 更新名称映射缓存
        Map<String, String> nameMap = new HashMap<>();
        for (DictItemVO vo : voList) {
            nameMap.put(vo.getItemCode(), vo.getItemName());
        }
        dictNameCache.put(dictCode, nameMap);
    }
    
    /**
     * 获取字典项列表
     */
    public List<DictItemVO> getDictItems(String dictCode) {
        List<DictItemVO> result = dictCache.get(dictCode);
        if (result == null) {
            refreshDictCache(dictCode);
            result = dictCache.get(dictCode);
        }
        return result != null ? result : Collections.emptyList();
    }
    
    /**
     * 获取字典项名称
     */
    public String getDictItemName(String dictCode, String itemCode) {
        Map<String, String> nameMap = dictNameCache.get(dictCode);
        if (nameMap == null) {
            refreshDictCache(dictCode);
            nameMap = dictNameCache.get(dictCode);
        }
        if (nameMap != null) {
            return nameMap.getOrDefault(itemCode, itemCode);
        }
        return itemCode;
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
            dictCache.remove(type.getDictCode());
            dictNameCache.remove(type.getDictCode());
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
     * 字典项VO
     */
    public static class DictItemVO {
        private String itemCode;
        private String itemName;
        private String itemValue;
        private Integer sort;
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
