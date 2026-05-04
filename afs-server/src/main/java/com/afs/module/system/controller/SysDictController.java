package com.afs.module.system.controller;

import com.afs.common.Result;
import com.afs.module.system.entity.SysDictItem;
import com.afs.module.system.entity.SysDictType;
import com.afs.module.system.service.SysDictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 字典管理接口
 */
@RestController
@RequestMapping("/api/sys/dict")
public class SysDictController {
    
    @Autowired
    private SysDictService dictService;
    
    // ========================================
    // 字典查询接口（供业务使用）
    // ========================================
    
    /**
     * 获取字典项列表
     * GET /api/sys/dict/{dictCode}
     */
    @GetMapping("/{dictCode}")
    public Result<List<SysDictService.DictItemVO>> getDictItems(@PathVariable String dictCode) {
        return Result.success(dictService.getDictItems(dictCode));
    }
    
    /**
     * 批量获取多个字典
     * POST /api/sys/dict/batch
     */
    @PostMapping("/batch")
    public Result<Map<String, List<SysDictService.DictItemVO>>> getBatchDicts(@RequestBody List<String> dictCodes) {
        return Result.success(dictService.getBatchDicts(dictCodes));
    }
    
    /**
     * 获取字典项名称
     * GET /api/sys/dict/name/{dictCode}/{itemCode}
     */
    @GetMapping("/name/{dictCode}/{itemCode}")
    public Result<String> getDictItemName(@PathVariable String dictCode, @PathVariable String itemCode) {
        return Result.success(dictService.getDictItemName(dictCode, itemCode));
    }
    
    // ========================================
    // 字典管理接口（后台管理使用）
    // ========================================
    
    /**
     * 获取所有字典类型
     * GET /api/sys/dict/types
     */
    @GetMapping("/types")
    public Result<List<SysDictType>> getAllDictTypes() {
        return Result.success(dictService.getAllDictTypes());
    }
    
    /**
     * 创建字典类型
     * POST /api/sys/dict/type
     */
    @PostMapping("/type")
    public Result<SysDictType> createDictType(@RequestBody SysDictType dictType) {
        return Result.success(dictService.createDictType(dictType));
    }
    
    /**
     * 更新字典类型
     * PUT /api/sys/dict/type/{id}
     */
    @PutMapping("/type/{id}")
    public Result<SysDictType> updateDictType(@PathVariable Long id, @RequestBody SysDictType dictType) {
        dictType.setId(id);
        return Result.success(dictService.updateDictType(dictType));
    }
    
    /**
     * 删除字典类型
     * DELETE /api/sys/dict/type/{id}
     */
    @DeleteMapping("/type/{id}")
    public Result<Void> deleteDictType(@PathVariable Long id) {
        dictService.deleteDictType(id);
        return Result.success();
    }
    
    /**
     * 创建字典项
     * POST /api/sys/dict/item
     */
    @PostMapping("/item")
    public Result<SysDictItem> createDictItem(@RequestBody SysDictItem dictItem) {
        return Result.success(dictService.createDictItem(dictItem));
    }
    
    /**
     * 更新字典项
     * PUT /api/sys/dict/item/{id}
     */
    @PutMapping("/item/{id}")
    public Result<SysDictItem> updateDictItem(@PathVariable Long id, @RequestBody SysDictItem dictItem) {
        dictItem.setId(id);
        return Result.success(dictService.updateDictItem(dictItem));
    }
    
    /**
     * 删除字典项
     * DELETE /api/sys/dict/item/{id}
     */
    @DeleteMapping("/item/{id}")
    public Result<Void> deleteDictItem(@PathVariable Long id) {
        dictService.deleteDictItem(id);
        return Result.success();
    }
    
    /**
     * 刷新缓存
     * POST /api/sys/dict/refresh
     */
    @PostMapping("/refresh")
    public Result<Void> refreshCache() {
        dictService.refreshAllCache();
        return Result.success();
    }
    
    /**
     * 刷新指定字典缓存
     * POST /api/sys/dict/refresh/{dictCode}
     */
    @PostMapping("/refresh/{dictCode}")
    public Result<Void> refreshDictCache(@PathVariable String dictCode) {
        dictService.refreshDictCache(dictCode);
        return Result.success();
    }
}
