package com.afs.module.system.service;

import com.afs.module.system.entity.SysDictItem;
import com.afs.module.system.entity.SysDictType;
import com.afs.module.system.mapper.SysDictItemMapper;
import com.afs.module.system.mapper.SysDictTypeMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SysDictService 单元测试
 *
 * 覆盖字典 Redis 缓存（内存 Map 模拟有状态 Redis：写后可读、删后未命中）的加载/刷新/失效、
 * 字典项查询与名称映射回退，以及字典类型/字典项的增删改对缓存的联动刷新。
 */
@ExtendWith(MockitoExtension.class)
class SysDictServiceTest {

    @Mock
    private SysDictTypeMapper dictTypeMapper;

    @Mock
    private SysDictItemMapper dictItemMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private SysDictService dictService;

    // 内存 Map 模拟 Redis 存储，保证"写后能读、删后未命中"的真实缓存语义
    private final Map<String, String> redisStore = new HashMap<>();

    @BeforeEach
    void setUp() {
        redisStore.clear();
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get(anyString()))
                .thenAnswer(inv -> redisStore.get(inv.getArgument(0)));
        lenient().doAnswer(inv -> {
            redisStore.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        lenient().doAnswer(inv -> {
            redisStore.remove(inv.getArgument(0));
            return null;
        }).when(redisTemplate).delete(anyString());
        dictService = new SysDictService(dictTypeMapper, dictItemMapper, redisTemplate, new ObjectMapper());
    }

    // 刷新全部缓存：仅为启用状态的类型加载字典项
    @Test
    void refreshAllCacheLoadsEnabledTypes() {
        SysDictType type = buildType(1L, "scam_type");
        when(dictTypeMapper.selectList(any())).thenReturn(List.of(type));
        when(dictItemMapper.selectList(any())).thenReturn(List.of(buildItem("scam_type", "telecom", "电信诈骗")));

        dictService.refreshAllCache();

        List<SysDictService.DictItemVO> items = dictService.getDictItems("scam_type");
        assertEquals(1, items.size());
        assertEquals("电信诈骗", items.get(0).getItemName());
        // 命中缓存后不再查询字典项表
        verify(dictItemMapper, times(1)).selectList(any());
    }

    // 获取字典项：缓存未命中时自动从数据库加载
    @Test
    void getDictItemsLoadsFromDbOnCacheMiss() {
        when(dictItemMapper.selectList(any())).thenReturn(List.of(buildItem("gender", "m", "男")));

        List<SysDictService.DictItemVO> result = dictService.getDictItems("gender");

        assertEquals(1, result.size());
        assertEquals("m", result.get(0).getItemCode());
    }

    // 获取字典项：数据库无数据时返回空列表
    @Test
    void getDictItemsReturnsEmptyListWhenNoData() {
        when(dictItemMapper.selectList(any())).thenReturn(List.of());

        assertTrue(dictService.getDictItems("unknown").isEmpty());
    }

    // 获取字典项名称：命中映射时返回名称
    @Test
    void getDictItemNameReturnsMappedName() {
        when(dictItemMapper.selectList(any())).thenReturn(List.of(buildItem("gender", "m", "男")));

        assertEquals("男", dictService.getDictItemName("gender", "m"));
    }

    // 获取字典项名称：映射中无该 itemCode 时回退返回 itemCode 本身
    @Test
    void getDictItemNameFallsBackToItemCode() {
        when(dictItemMapper.selectList(any())).thenReturn(List.of(buildItem("gender", "m", "男")));

        assertEquals("x", dictService.getDictItemName("gender", "x"));
    }

    // 批量获取多个字典
    @Test
    void getBatchDictsReturnsPerCodeItems() {
        when(dictItemMapper.selectList(any())).thenReturn(List.of(buildItem("d1", "a", "A")));

        Map<String, List<SysDictService.DictItemVO>> result = dictService.getBatchDicts(List.of("d1", "d2"));

        assertEquals(2, result.size());
        assertEquals(1, result.get("d1").size());
    }

    // 获取全部字典类型
    @Test
    void getAllDictTypesReturnsMapperResult() {
        when(dictTypeMapper.selectList(any())).thenReturn(List.of(buildType(1L, "scam_type")));

        assertEquals(1, dictService.getAllDictTypes().size());
    }

    // 创建字典类型：自动填充创建/更新时间
    @Test
    void createDictTypeFillsTimestamps() {
        SysDictType type = buildType(null, "new_dict");

        SysDictType result = dictService.createDictType(type);

        assertNotNull(result.getCreateTime());
        assertNotNull(result.getUpdateTime());
        verify(dictTypeMapper).insert(type);
    }

    // 更新字典类型：更新后刷新对应字典缓存
    @Test
    void updateDictTypeRefreshesCache() {
        SysDictType type = buildType(1L, "scam_type");
        when(dictItemMapper.selectList(any())).thenReturn(List.of());

        dictService.updateDictType(type);

        verify(dictTypeMapper).updateById(type);
        verify(dictItemMapper).selectList(any());
    }

    // 删除字典类型：级联删除字典项并清除缓存
    @Test
    void deleteDictTypeCascadesAndEvictsCache() {
        when(dictTypeMapper.selectById(1L)).thenReturn(buildType(1L, "scam_type"));

        dictService.deleteDictType(1L);

        verify(dictItemMapper).delete(any());
        verify(dictTypeMapper).deleteById(1L);
    }

    // 删除字典类型：类型不存在时不做任何删除
    @Test
    void deleteDictTypeDoesNothingWhenMissing() {
        when(dictTypeMapper.selectById(1L)).thenReturn(null);

        dictService.deleteDictType(1L);

        verify(dictItemMapper, never()).delete(any());
        verify(dictTypeMapper, never()).deleteById(any(Long.class));
    }

    // 创建字典项：插入后刷新对应缓存
    @Test
    void createDictItemInsertsAndRefreshesCache() {
        SysDictItem item = buildItem("scam_type", "telecom", "电信诈骗");
        when(dictItemMapper.selectList(any())).thenReturn(List.of(item));

        SysDictItem result = dictService.createDictItem(item);

        assertNotNull(result.getCreateTime());
        verify(dictItemMapper).insert(item);
        verify(dictItemMapper).selectList(any());
    }

    // 更新字典项：更新后刷新对应缓存
    @Test
    void updateDictItemUpdatesAndRefreshesCache() {
        SysDictItem item = buildItem("scam_type", "telecom", "电信诈骗");
        when(dictItemMapper.selectList(any())).thenReturn(List.of(item));

        dictService.updateDictItem(item);

        verify(dictItemMapper).updateById(item);
        verify(dictItemMapper).selectList(any());
    }

    // 删除字典项：删除后刷新对应缓存
    @Test
    void deleteDictItemDeletesAndRefreshesCache() {
        when(dictItemMapper.selectById(1L)).thenReturn(buildItem("scam_type", "telecom", "电信诈骗"));
        when(dictItemMapper.selectList(any())).thenReturn(List.of());

        dictService.deleteDictItem(1L);

        verify(dictItemMapper).deleteById(1L);
        verify(dictItemMapper).selectList(any());
    }

    // 删除字典项：字典项不存在时不做任何操作
    @Test
    void deleteDictItemDoesNothingWhenMissing() {
        when(dictItemMapper.selectById(1L)).thenReturn(null);

        dictService.deleteDictItem(1L);

        verify(dictItemMapper, never()).deleteById(any(Long.class));
        verify(dictItemMapper, never()).selectList(any());
    }

    private SysDictType buildType(Long id, String dictCode) {
        SysDictType type = new SysDictType();
        type.setId(id);
        type.setDictCode(dictCode);
        type.setDictName(dictCode);
        type.setStatus(1);
        return type;
    }

    private SysDictItem buildItem(String dictCode, String itemCode, String itemName) {
        SysDictItem item = new SysDictItem();
        item.setDictCode(dictCode);
        item.setItemCode(itemCode);
        item.setItemName(itemName);
        item.setSort(1);
        item.setStatus(1);
        return item;
    }
}
