package com.afs.module.knowledge.service;

import com.afs.exception.BusinessException;
import com.afs.module.knowledge.entity.Knowledge;
import com.afs.module.knowledge.mapper.KnowledgeAuditMapper;
import com.afs.module.knowledge.mapper.KnowledgeMapper;
import com.afs.module.knowledge.mapper.KnowledgeTagMapper;
import com.afs.module.knowledge.mapper.KnowledgeTagRelationMapper;
import com.afs.module.knowledge.mapper.KnowledgeVersionMapper;
import com.afs.module.knowledge.vo.BatchUploadResultVO;
import com.afs.module.rag.ingestion.RagIngestionDispatcher;
import com.afs.module.rag.ingestion.RagIngestionService;
import com.afs.module.rag.search.RagSearchService;
import com.afs.util.DocumentParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * KnowledgeService 文档上传单元测试
 *
 * 覆盖：单文件上传的校验分支（空文件、空文件名、不支持格式、超大文件、
 * 解析失败、内容为空）与成功路径（标题/分类自动补全、内容空白压缩），
 * 以及批量上传的空数组异常、全部成功、部分成功、全部失败分支。
 * DocumentParser 为静态工具类，统一通过 MockedStatic 打桩，不做真实文件解析。
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeServiceUploadTest {

    @Mock
    private KnowledgeMapper knowledgeMapper;
    @Mock
    private KnowledgeTagMapper tagMapper;
    @Mock
    private KnowledgeTagRelationMapper tagRelationMapper;
    @Mock
    private KnowledgeVersionMapper versionMapper;
    @Mock
    private KnowledgeAuditMapper auditMapper;
    @Mock
    private RagIngestionService ragIngestionService;
    @Mock
    private RagIngestionDispatcher ragIngestionDispatcher;
    @Mock
    private RagSearchService ragSearchService;

    @InjectMocks
    private KnowledgeService knowledgeService;

    // 构造一个合法的基础文件 mock（宽松打桩，部分校验分支不会走到 getSize）
    private MultipartFile validFile(String fileName) {
        MultipartFile file = mock(MultipartFile.class);
        Mockito.lenient().when(file.isEmpty()).thenReturn(false);
        Mockito.lenient().when(file.getOriginalFilename()).thenReturn(fileName);
        Mockito.lenient().when(file.getSize()).thenReturn(1024L);
        return file;
    }

    // 空文件直接拒绝
    @Test
    void uploadDocument_emptyFile_throwsBadRequest() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> knowledgeService.uploadDocument(file, "分类", "标题"));

        assertEquals("请选择要上传的文件", exception.getMessage());
        verifyNoInteractions(knowledgeMapper);
    }

    // 文件名为 null 拒绝
    @Test
    void uploadDocument_nullFileName_throwsBadRequest() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> knowledgeService.uploadDocument(file, "分类", "标题"));

        assertEquals("文件名不能为空", exception.getMessage());
    }

    // 不支持的扩展名拒绝
    @Test
    void uploadDocument_unsupportedExtension_throwsBadRequest() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> knowledgeService.uploadDocument(validFile("evil.exe"), "分类", "标题"));

        assertTrue(exception.getMessage().contains("不支持的文件格式"));
    }

    // 超过 10MB 拒绝
    @Test
    void uploadDocument_oversizedFile_throwsBadRequest() {
        MultipartFile file = validFile("big.pdf");
        when(file.getSize()).thenReturn(11L * 1024 * 1024);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> knowledgeService.uploadDocument(file, "分类", "标题"));

        assertEquals("文件大小超过限制（最大 10MB）", exception.getMessage());
    }

    // 解析器抛异常时包装为业务异常
    @Test
    void uploadDocument_parseFailure_throwsBadRequest() {
        try (MockedStatic<DocumentParser> parser = Mockito.mockStatic(DocumentParser.class)) {
            // 打桩：解析器抛出 IO 异常
            parser.when(() -> DocumentParser.parseDocument(any()))
                    .thenThrow(new IOException("解析失败"));

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> knowledgeService.uploadDocument(validFile("a.pdf"), "分类", "标题"));

            assertTrue(exception.getMessage().startsWith("文档处理失败"));
        }
    }

    // 解析内容为空（纯空白）拒绝
    @Test
    void uploadDocument_blankContent_throwsBadRequest() {
        try (MockedStatic<DocumentParser> parser = Mockito.mockStatic(DocumentParser.class)) {
            parser.when(() -> DocumentParser.parseDocument(any()))
                    .thenReturn(Map.of("content", "   ", "documentType", "pdf", "charCount", 0));

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> knowledgeService.uploadDocument(validFile("a.pdf"), "分类", "标题"));

            assertEquals("无法从文档中提取内容", exception.getMessage());
        }
    }

    // 成功上传：标题与分类为空时自动补全，内容压缩空白，字段完整落库
    @Test
    void uploadDocument_success_autoFillsTitleCategoryAndNormalizesContent() {
        try (MockedStatic<DocumentParser> parser = Mockito.mockStatic(DocumentParser.class)) {
            // 打桩：解析结果含多余空白，标题/分类由工具类推导
            parser.when(() -> DocumentParser.parseDocument(any()))
                    .thenReturn(Map.of("content", "  第一行\n  第二行  ", "documentType", "txt", "charCount", 8));
            parser.when(() -> DocumentParser.extractTitle("a.txt")).thenReturn("自动标题");
            parser.when(() -> DocumentParser.suggestCategory(any(), any())).thenReturn("自动分类");

            Knowledge result = knowledgeService.uploadDocument(validFile("a.txt"), "  ", null);

            assertEquals("自动标题", result.getTitle());
            assertEquals("自动分类", result.getCategory());
            assertEquals("第一行 第二行", result.getContent());
            assertEquals("txt", result.getDocumentType());
            assertEquals("a.txt", result.getSourceFile());
            assertEquals(1024L, result.getFileSize());
            assertEquals(8, result.getCharCount());
            // 新增走 addKnowledge，默认 ACTIVE 并派发 MQ 入库任务（id 由数据库生成，mock 下为 null）
            verify(knowledgeMapper).insert(result);
            verify(ragIngestionDispatcher).dispatchKnowledgeUpsert(isNull());
        }
    }

    // 显式传入标题与分类时不触发自动补全
    @Test
    void uploadDocument_explicitTitleAndCategory_keepsGivenValues() {
        try (MockedStatic<DocumentParser> parser = Mockito.mockStatic(DocumentParser.class)) {
            parser.when(() -> DocumentParser.parseDocument(any()))
                    .thenReturn(Map.of("content", "内容", "documentType", "md", "charCount", 2));

            Knowledge result = knowledgeService.uploadDocument(validFile("a.md"), "指定分类", "指定标题");

            assertEquals("指定标题", result.getTitle());
            assertEquals("指定分类", result.getCategory());
            // 未调用自动补全的静态方法
            parser.verify(() -> DocumentParser.extractTitle(any()), Mockito.never());
            parser.verify(() -> DocumentParser.suggestCategory(any(), any()), Mockito.never());
        }
    }

    // 批量上传：空数组与 null 均拒绝
    @Test
    void uploadDocumentsBatch_emptyOrNullFiles_throwsBadRequest() {
        assertThrows(BusinessException.class,
                () -> knowledgeService.uploadDocumentsBatch(new MultipartFile[0], "分类"));
        assertThrows(BusinessException.class,
                () -> knowledgeService.uploadDocumentsBatch(null, "分类"));
    }

    // 批量上传全部成功
    @Test
    void uploadDocumentsBatch_allSuccess() {
        try (MockedStatic<DocumentParser> parser = Mockito.mockStatic(DocumentParser.class)) {
            parser.when(() -> DocumentParser.parseDocument(any()))
                    .thenReturn(Map.of("content", "内容", "documentType", "txt", "charCount", 2));
            parser.when(() -> DocumentParser.extractTitle(any())).thenReturn("标题");
            parser.when(() -> DocumentParser.suggestCategory(any(), any())).thenReturn("分类");

            BatchUploadResultVO result = knowledgeService.uploadDocumentsBatch(
                    new MultipartFile[]{validFile("a.txt"), validFile("b.txt")}, null);

            assertEquals(2, result.getSuccessCount());
            assertEquals(0, result.getFailCount());
            assertEquals("", result.getFailMessages());
        }
    }

    // 批量上传部分失败：失败信息拼接文件名与原因，成功继续处理
    @Test
    void uploadDocumentsBatch_partialFailure_collectsFailMessages() {
        try (MockedStatic<DocumentParser> parser = Mockito.mockStatic(DocumentParser.class)) {
            parser.when(() -> DocumentParser.parseDocument(any()))
                    .thenReturn(Map.of("content", "内容", "documentType", "txt", "charCount", 2));
            parser.when(() -> DocumentParser.extractTitle(any())).thenReturn("标题");
            parser.when(() -> DocumentParser.suggestCategory(any(), any())).thenReturn("分类");

            BatchUploadResultVO result = knowledgeService.uploadDocumentsBatch(
                    new MultipartFile[]{validFile("good.txt"), validFile("bad.exe")}, null);

            assertEquals(1, result.getSuccessCount());
            assertEquals(1, result.getFailCount());
            assertTrue(result.getFailMessages().contains("bad.exe"));
            assertTrue(result.getFailMessages().contains("不支持的文件格式"));
        }
    }

    // 批量上传全部失败
    @Test
    void uploadDocumentsBatch_allFail() {
        BatchUploadResultVO result = knowledgeService.uploadDocumentsBatch(
                new MultipartFile[]{validFile("a.exe"), validFile("b.exe")}, null);

        assertEquals(0, result.getSuccessCount());
        assertEquals(2, result.getFailCount());
        assertTrue(result.getFailMessages().contains("a.exe"));
        assertTrue(result.getFailMessages().contains("b.exe"));
        // 失败信息之间使用分号分隔
        assertTrue(result.getFailMessages().contains("; "));
    }

    // 上传成功后落库实体带默认 ACTIVE 状态（通过捕获 insert 参数验证）
    @Test
    void uploadDocument_success_persistsActiveStatus() {
        try (MockedStatic<DocumentParser> parser = Mockito.mockStatic(DocumentParser.class)) {
            parser.when(() -> DocumentParser.parseDocument(any()))
                    .thenReturn(Map.of("content", "内容", "documentType", "pdf", "charCount", 2));

            knowledgeService.uploadDocument(validFile("a.pdf"), "分类", "标题");

            ArgumentCaptor<Knowledge> captor = ArgumentCaptor.forClass(Knowledge.class);
            verify(knowledgeMapper).insert(captor.capture());
            assertEquals("ACTIVE", captor.getValue().getStatus());
        }
    }
}
