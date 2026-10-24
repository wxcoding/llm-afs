package com.afs.module.knowledge.controller;

import com.afs.exception.BusinessException;
import com.afs.module.knowledge.entity.Knowledge;
import com.afs.module.knowledge.entity.KnowledgeAudit;
import com.afs.module.knowledge.entity.KnowledgeTag;
import com.afs.module.knowledge.entity.KnowledgeVersion;
import com.afs.module.knowledge.service.KnowledgeService;
import com.afs.module.knowledge.vo.BatchUploadResultVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * KnowledgeController 单元测试
 *
 * 基于 MockMvc standaloneSetup（不启动 Spring 容器），mock KnowledgeService，
 * 覆盖全部端点的正常路径：基础 CRUD、语义搜索、向量同步、单文件/批量上传
 * （含全部成功/部分成功/全部失败三种消息分支）、支持类型查询、
 * 标签管理、版本历史/回滚、审核提交/待审核列表/通过/拒绝；
 * 异常路径：standalone 模式未注册全局异常处理器，业务异常向外抛出。
 */
class KnowledgeControllerTest {

    private KnowledgeService knowledgeService;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        knowledgeService = mock(KnowledgeService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new KnowledgeController(knowledgeService)).build();
    }

    // ==================== 基础 CRUD ====================

    // 无筛选参数时查询全部知识
    @Test
    void getKnowledge_noParams_callsGetAll() throws Exception {
        Knowledge knowledge = new Knowledge();
        knowledge.setTitle("防骗指南");
        when(knowledgeService.getAllKnowledge()).thenReturn(List.of(knowledge));

        mockMvc.perform(get("/api/knowledge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("防骗指南"));
        verify(knowledgeService).getAllKnowledge();
    }

    // 带分类参数时按分类查询
    @Test
    void getKnowledge_withCategory_callsGetByCategory() throws Exception {
        when(knowledgeService.getKnowledgeByCategory("诈骗类型")).thenReturn(List.of(new Knowledge()));

        mockMvc.perform(get("/api/knowledge").param("category", "诈骗类型"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(knowledgeService).getKnowledgeByCategory("诈骗类型");
    }

    // 带关键词参数时走关键词搜索（关键词优先于分类）
    @Test
    void getKnowledge_withKeyword_callsSearch() throws Exception {
        when(knowledgeService.searchKnowledge("刷单")).thenReturn(List.of(new Knowledge()));

        mockMvc.perform(get("/api/knowledge").param("keyword", "刷单").param("category", "诈骗类型"))
                .andExpect(status().isOk());
        verify(knowledgeService).searchKnowledge("刷单");
    }

    // 按 ID 查询知识
    @Test
    void getKnowledgeById_returnsKnowledge() throws Exception {
        Knowledge knowledge = new Knowledge();
        knowledge.setTitle("标题");
        when(knowledgeService.getKnowledgeById(1L)).thenReturn(knowledge);

        mockMvc.perform(get("/api/knowledge/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("标题"));
    }

    // service 抛出业务异常时，standalone 模式无异常处理器，异常向外抛出
    @Test
    void getKnowledgeById_serviceThrows_propagatesException() {
        when(knowledgeService.getKnowledgeById(9L)).thenThrow(BusinessException.notFound("知识不存在"));

        assertThrows(ServletException.class,
                () -> mockMvc.perform(get("/api/knowledge/{id}", 9L)));
    }

    // 新增知识：请求体字段映射到实体并调用 service
    @Test
    void addKnowledge_mapsBodyAndCallsService() throws Exception {
        Knowledge saved = new Knowledge();
        saved.setTitle("标题");
        when(knowledgeService.addKnowledge(any(Knowledge.class))).thenReturn(saved);

        mockMvc.perform(post("/api/knowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "标题", "category", "分类", "content", "内容"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("添加成功"))
                .andExpect(jsonPath("$.data.title").value("标题"));
        verify(knowledgeService).addKnowledge(any(Knowledge.class));
    }

    // 更新知识
    @Test
    void updateKnowledge_callsService() throws Exception {
        when(knowledgeService.updateKnowledge(eq(1L), any(Knowledge.class))).thenReturn(new Knowledge());

        mockMvc.perform(put("/api/knowledge/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "新标题"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("更新成功"));
        verify(knowledgeService).updateKnowledge(eq(1L), any(Knowledge.class));
    }

    // 删除知识
    @Test
    void deleteKnowledge_callsService() throws Exception {
        mockMvc.perform(delete("/api/knowledge/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("删除成功"));
        verify(knowledgeService).deleteKnowledge(1L);
    }

    // ==================== 搜索与向量同步 ====================

    // 语义搜索：默认 topK 与显式 topK
    @Test
    void semanticSearch_passesQueryAndTopK() throws Exception {
        when(knowledgeService.semanticSearch("诈骗", 3))
                .thenReturn(List.of(Map.of("id", 1L, "score", 0.9)));

        mockMvc.perform(get("/api/knowledge/search/semantic")
                        .param("query", "诈骗").param("topK", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1));
        verify(knowledgeService).semanticSearch("诈骗", 3);
    }

    // 触发全量向量同步
    @Test
    void syncVectorStore_callsService() throws Exception {
        mockMvc.perform(post("/api/knowledge/sync-vector"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("同步完成"));
        verify(knowledgeService).syncAllToVectorStore();
    }

    // ==================== 文档上传 ====================

    // 单文件上传
    @Test
    void uploadDocument_callsServiceWithParams() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "内容".getBytes());
        Knowledge saved = new Knowledge();
        saved.setTitle("标题");
        when(knowledgeService.uploadDocument(any(MultipartFile.class), eq("分类"), eq("标题")))
                .thenReturn(saved);

        mockMvc.perform(multipart("/api/knowledge/upload").file(file)
                        .param("category", "分类").param("title", "标题"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("文档上传成功"))
                .andExpect(jsonPath("$.data.title").value("标题"));
    }

    // 批量上传：全部成功时的消息
    @Test
    void uploadBatch_allSuccess_message() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "a.txt", "text/plain", "x".getBytes());
        when(knowledgeService.uploadDocumentsBatch(any(MultipartFile[].class), isNull()))
                .thenReturn(new BatchUploadResultVO(2, 0, ""));

        mockMvc.perform(multipart("/api/knowledge/upload/batch").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("全部上传成功"))
                .andExpect(jsonPath("$.data.successCount").value(2));
    }

    // 批量上传：部分成功时的消息
    @Test
    void uploadBatch_partialFailure_message() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "a.txt", "text/plain", "x".getBytes());
        when(knowledgeService.uploadDocumentsBatch(any(MultipartFile[].class), isNull()))
                .thenReturn(new BatchUploadResultVO(1, 1, "b.exe: 不支持的文件格式"));

        mockMvc.perform(multipart("/api/knowledge/upload/batch").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("部分上传成功"))
                .andExpect(jsonPath("$.data.failCount").value(1));
    }

    // 批量上传：全部失败时的消息
    @Test
    void uploadBatch_allFail_message() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "a.exe", "text/plain", "x".getBytes());
        when(knowledgeService.uploadDocumentsBatch(any(MultipartFile[].class), isNull()))
                .thenReturn(new BatchUploadResultVO(0, 1, "a.exe: 不支持的文件格式"));

        mockMvc.perform(multipart("/api/knowledge/upload/batch").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("全部上传失败"));
    }

    // 查询支持的文件类型列表
    @Test
    void getSupportedTypes_returnsTypeList() throws Exception {
        mockMvc.perform(get("/api/knowledge/supported-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.maxFileSizeMB").value(10))
                .andExpect(jsonPath("$.data.types.length()").value(8))
                .andExpect(jsonPath("$.data.types[0].extension").value(".pdf"));
    }

    // ==================== 标签管理 ====================

    // 创建标签：DTO 字段透传给 service
    @Test
    void createTag_mapsBodyAndCallsService() throws Exception {
        KnowledgeTag tag = new KnowledgeTag();
        tag.setName("高危");
        when(knowledgeService.createTag("高危", "red", "描述")).thenReturn(tag);

        mockMvc.perform(post("/api/knowledge/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "高危", "color", "red", "description", "描述"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("高危"));
        verify(knowledgeService).createTag("高危", "red", "描述");
    }

    // 查询全部标签
    @Test
    void getAllTags_returnsList() throws Exception {
        KnowledgeTag tag = new KnowledgeTag();
        tag.setName("高危");
        when(knowledgeService.getAllTags()).thenReturn(List.of(tag));

        mockMvc.perform(get("/api/knowledge/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("高危"));
    }

    // 删除标签
    @Test
    void deleteTag_callsService() throws Exception {
        mockMvc.perform(delete("/api/knowledge/tags/{tagId}", 5L))
                .andExpect(status().isOk());
        verify(knowledgeService).deleteTag(5L);
    }

    // 为知识绑定标签
    @Test
    void addTagToKnowledge_callsService() throws Exception {
        mockMvc.perform(post("/api/knowledge/tags/knowledge/{knowledgeId}", 10L)
                        .param("tagId", "5"))
                .andExpect(status().isOk());
        verify(knowledgeService).addTagToKnowledge(10L, 5L);
    }

    // 解绑知识标签
    @Test
    void removeTagFromKnowledge_callsService() throws Exception {
        mockMvc.perform(delete("/api/knowledge/tags/knowledge/{knowledgeId}", 10L)
                        .param("tagId", "5"))
                .andExpect(status().isOk());
        verify(knowledgeService).removeTagFromKnowledge(10L, 5L);
    }

    // 查询知识关联的标签
    @Test
    void getTagsForKnowledge_returnsList() throws Exception {
        when(knowledgeService.getTagsForKnowledge(10L)).thenReturn(List.of(new KnowledgeTag()));

        mockMvc.perform(get("/api/knowledge/tags/knowledge/{knowledgeId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    // ==================== 版本控制 ====================

    // 查询版本历史
    @Test
    void getVersionHistory_returnsList() throws Exception {
        KnowledgeVersion version = new KnowledgeVersion();
        version.setTitle("历史标题");
        when(knowledgeService.getVersionHistory(1L)).thenReturn(List.of(version));

        mockMvc.perform(get("/api/knowledge/versions/{knowledgeId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("历史标题"));
    }

    // 回滚到指定版本
    @Test
    void rollbackToVersion_callsService() throws Exception {
        KnowledgeVersion version = new KnowledgeVersion();
        version.setTitle("目标版本");
        when(knowledgeService.rollbackToVersion(100L)).thenReturn(version);

        mockMvc.perform(post("/api/knowledge/versions/{versionId}/rollback", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("目标版本"));
        verify(knowledgeService).rollbackToVersion(100L);
    }

    // ==================== 审核流程 ====================

    // 提交审核：DTO 字段透传给 service
    @Test
    void submitForAudit_mapsBodyAndCallsService() throws Exception {
        KnowledgeAudit audit = new KnowledgeAudit();
        audit.setAuditStatus("pending");
        when(knowledgeService.submitForAudit(1L, "标题", "内容", "分类", 7L)).thenReturn(audit);

        mockMvc.perform(post("/api/knowledge/audit/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "knowledgeId", 1, "title", "标题", "content", "内容",
                                "category", "分类", "submitUserId", 7))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.auditStatus").value("pending"));
        verify(knowledgeService).submitForAudit(1L, "标题", "内容", "分类", 7L);
    }

    // 查询待审核列表
    @Test
    void getPendingAudits_returnsList() throws Exception {
        when(knowledgeService.getPendingAudits()).thenReturn(List.of(new KnowledgeAudit()));

        mockMvc.perform(get("/api/knowledge/audit/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    // 审核通过：审核人 ID 与意见透传
    @Test
    void approveAudit_callsService() throws Exception {
        KnowledgeAudit audit = new KnowledgeAudit();
        audit.setAuditStatus("approved");
        when(knowledgeService.approveAudit(1L, 8L, "同意")).thenReturn(audit);

        mockMvc.perform(post("/api/knowledge/audit/{auditId}/approve", 1L)
                        .param("auditUserId", "8").param("comment", "同意"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.auditStatus").value("approved"));
        verify(knowledgeService).approveAudit(1L, 8L, "同意");
    }

    // 审核拒绝
    @Test
    void rejectAudit_callsService() throws Exception {
        KnowledgeAudit audit = new KnowledgeAudit();
        audit.setAuditStatus("rejected");
        when(knowledgeService.rejectAudit(1L, 8L, "内容不准确")).thenReturn(audit);

        mockMvc.perform(post("/api/knowledge/audit/{auditId}/reject", 1L)
                        .param("auditUserId", "8").param("comment", "内容不准确"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.auditStatus").value("rejected"));
        verify(knowledgeService).rejectAudit(1L, 8L, "内容不准确");
    }

    // 审核通过缺少必填 auditUserId 参数时返回 400
    @Test
    void approveAudit_missingAuditUserId_returns400() throws Exception {
        mockMvc.perform(post("/api/knowledge/audit/{auditId}/approve", 1L))
                .andExpect(status().isBadRequest());
    }
}
