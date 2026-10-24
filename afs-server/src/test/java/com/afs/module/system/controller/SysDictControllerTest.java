package com.afs.module.system.controller;

import com.afs.module.system.entity.SysDictItem;
import com.afs.module.system.entity.SysDictType;
import com.afs.module.system.service.SysDictService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SysDictController 单元测试
 *
 * 基于 MockMvc standalone 模式验证字典查询接口（单项/批量/名称映射）
 * 与后台管理接口（类型、字典项的增删改、缓存刷新）。
 */
class SysDictControllerTest {

    private MockMvc mockMvc;

    private SysDictService dictService;

    @BeforeEach
    void setUp() {
        dictService = mock(SysDictService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SysDictController(dictService)).build();
    }

    // 获取指定字典的字典项列表
    @Test
    void getDictItemsReturnsItems() throws Exception {
        SysDictService.DictItemVO vo = new SysDictService.DictItemVO();
        vo.setItemCode("telecom");
        vo.setItemName("电信诈骗");
        when(dictService.getDictItems("scam_type")).thenReturn(List.of(vo));

        mockMvc.perform(get("/api/sys/dict/scam_type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].itemCode").value("telecom"))
                .andExpect(jsonPath("$.data[0].itemName").value("电信诈骗"));
    }

    // 批量获取多个字典
    @Test
    void getBatchDictsReturnsMap() throws Exception {
        SysDictService.DictItemVO vo = new SysDictService.DictItemVO();
        vo.setItemCode("telecom");
        vo.setItemName("电信诈骗");
        when(dictService.getBatchDicts(List.of("scam_type"))).thenReturn(Map.of("scam_type", List.of(vo)));

        mockMvc.perform(post("/api/sys/dict/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"scam_type\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scam_type[0].itemName").value("电信诈骗"));
    }

    // 获取字典项名称映射
    // 注意：Result.success(String) 存在重载，String 类型的 data 实际落在 message 字段
    @Test
    void getDictItemNameReturnsName() throws Exception {
        when(dictService.getDictItemName("scam_type", "telecom")).thenReturn("电信诈骗");

        mockMvc.perform(get("/api/sys/dict/name/scam_type/telecom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("电信诈骗"));
    }

    // 获取全部字典类型
    @Test
    void getAllDictTypesReturnsList() throws Exception {
        SysDictType type = new SysDictType();
        type.setDictCode("scam_type");
        when(dictService.getAllDictTypes()).thenReturn(List.of(type));

        mockMvc.perform(get("/api/sys/dict/types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].dictCode").value("scam_type"));
    }

    // 创建字典类型
    @Test
    void createDictTypeReturnsCreated() throws Exception {
        SysDictType type = new SysDictType();
        type.setDictCode("new_dict");
        when(dictService.createDictType(any(SysDictType.class))).thenReturn(type);

        mockMvc.perform(post("/api/sys/dict/type")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dictCode\":\"new_dict\",\"dictName\":\"新字典\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dictCode").value("new_dict"));
    }

    // 更新字典类型：路径 ID 会被写入实体
    @Test
    void updateDictTypeSetsIdFromPath() throws Exception {
        SysDictType type = new SysDictType();
        type.setId(5L);
        type.setDictCode("scam_type");
        when(dictService.updateDictType(any(SysDictType.class))).thenReturn(type);

        mockMvc.perform(put("/api/sys/dict/type/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dictCode\":\"scam_type\",\"dictName\":\"诈骗类型\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(5));
        // 校验 controller 将路径 ID 写入实体
        verify(dictService).updateDictType(argThat(t -> t.getId().equals(5L)));
    }

    // 删除字典类型
    @Test
    void deleteDictTypeReturnsSuccess() throws Exception {
        mockMvc.perform(delete("/api/sys/dict/type/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(dictService).deleteDictType(5L);
    }

    // 创建字典项
    @Test
    void createDictItemReturnsCreated() throws Exception {
        SysDictItem item = new SysDictItem();
        item.setItemCode("telecom");
        when(dictService.createDictItem(any(SysDictItem.class))).thenReturn(item);

        mockMvc.perform(post("/api/sys/dict/item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dictCode\":\"scam_type\",\"itemCode\":\"telecom\",\"itemName\":\"电信诈骗\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCode").value("telecom"));
    }

    // 更新字典项：路径 ID 会被写入实体
    @Test
    void updateDictItemSetsIdFromPath() throws Exception {
        SysDictItem item = new SysDictItem();
        item.setId(3L);
        when(dictService.updateDictItem(any(SysDictItem.class))).thenReturn(item);

        mockMvc.perform(put("/api/sys/dict/item/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dictCode\":\"scam_type\",\"itemCode\":\"telecom\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(3));
        verify(dictService).updateDictItem(argThat(i -> i.getId().equals(3L)));
    }

    // 删除字典项
    @Test
    void deleteDictItemReturnsSuccess() throws Exception {
        mockMvc.perform(delete("/api/sys/dict/item/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(dictService).deleteDictItem(3L);
    }

    // 刷新全部字典缓存
    @Test
    void refreshCacheReturnsSuccess() throws Exception {
        mockMvc.perform(post("/api/sys/dict/refresh"))
                .andExpect(status().isOk());
        verify(dictService).refreshAllCache();
    }

    // 刷新指定字典缓存
    @Test
    void refreshDictCacheReturnsSuccess() throws Exception {
        mockMvc.perform(post("/api/sys/dict/refresh/scam_type"))
                .andExpect(status().isOk());
        verify(dictService).refreshDictCache("scam_type");
    }
}
