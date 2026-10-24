package com.afs.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DocumentParser 单元测试
 *
 * 覆盖 txt / md / pdf / docx / doc / xlsx 各格式的解析、
 * 按扩展名兜底识别类型、不支持格式走 Tika 分支、损坏文件抛出
 * IOException，以及 extractTitle / suggestCategory 辅助方法。
 * 真实样本文件通过 @TempDir 临时目录生成，测试结束自动清理。
 */
class DocumentParserTest {

    @TempDir
    Path tempDir;

    // ---------- 各格式解析 ----------

    @Test
    // 纯文本文件：内容原样返回，类型为 text
    void parsesPlainTextFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "宣传.txt", "text/plain", "反诈宣传文本".getBytes(StandardCharsets.UTF_8));

        Map<String, Object> result = DocumentParser.parseDocument(file);

        assertThat(result.get("documentType")).isEqualTo("text");
        assertThat(result.get("content")).isEqualTo("反诈宣传文本");
        assertThat(result.get("fileName")).isEqualTo("宣传.txt");
        assertThat(result.get("charCount")).isEqualTo(6);
    }

    @Test
    // 空文本文件：内容为空串，charCount 为 0
    void parsesEmptyTextFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]);

        Map<String, Object> result = DocumentParser.parseDocument(file);

        assertThat(result.get("content")).isEqualTo("");
        assertThat(result.get("charCount")).isEqualTo(0);
    }

    @Test
    // Markdown 文件：标记被渲染为纯文本
    void parsesMarkdownFile() throws Exception {
        byte[] bytes = "# 防范指南\n\n**不要**轻信陌生来电".getBytes(StandardCharsets.UTF_8);
        Files.write(tempDir.resolve("guide.md"), bytes);
        MockMultipartFile file = new MockMultipartFile("file", "guide.md", "text/markdown", bytes);

        Map<String, Object> result = DocumentParser.parseDocument(file);

        assertThat(result.get("documentType")).isEqualTo("markdown");
        assertThat((String) result.get("content")).contains("防范指南").contains("不要");
        // Markdown 标记不应残留在纯文本结果中
        assertThat((String) result.get("content")).doesNotContain("#").doesNotContain("**");
    }

    @Test
    // PDF 文件：使用 PDFBox 编程生成一页文字后解析
    void parsesPdfFile() throws Exception {
        Path pdfPath = tempDir.resolve("notice.pdf");
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(100, 700);
                cs.showText("Anti Fraud Notice");
                cs.endText();
            }
            doc.save(pdfPath.toFile());
        }
        byte[] bytes = Files.readAllBytes(pdfPath);
        MockMultipartFile file = new MockMultipartFile("file", "notice.pdf", "application/pdf", bytes);

        Map<String, Object> result = DocumentParser.parseDocument(file);

        assertThat(result.get("documentType")).isEqualTo("pdf");
        assertThat((String) result.get("content")).contains("Anti Fraud Notice");
    }

    @Test
    // docx 文件：XWPFDocument 生成段落后解析
    void parsesDocxFile() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph paragraph = doc.createParagraph();
            paragraph.createRun().setText("Hello DOCX world");
            doc.write(baos);
        }
        byte[] bytes = baos.toByteArray();
        Files.write(tempDir.resolve("report.docx"), bytes);
        MockMultipartFile file = new MockMultipartFile("file", "report.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", bytes);

        Map<String, Object> result = DocumentParser.parseDocument(file);

        assertThat(result.get("documentType")).isEqualTo("word");
        assertThat((String) result.get("content")).contains("Hello DOCX world");
    }

    @Test
    // .doc 扩展名 + docx 内容：仍识别为 word 类型并经 XWPF 主路径成功解析
    void parsesDocExtensionWithDocxContent() throws Exception {
        byte[] bytes;
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            doc.createParagraph().createRun().setText("Legacy name docx content");
            doc.write(baos);
            bytes = baos.toByteArray();
        }
        Files.write(tempDir.resolve("legacy.doc"), bytes);
        MockMultipartFile file = new MockMultipartFile("file", "legacy.doc", "application/msword", bytes);

        Map<String, Object> result = DocumentParser.parseDocument(file);

        assertThat(result.get("documentType")).isEqualTo("word");
        assertThat((String) result.get("content")).contains("Legacy name docx content");
    }

    @Test
    // 损坏的 .doc 文件：XWPF 与 HWPF 回退均失败，统一包装为 IOException
    void corruptedLegacyDocThrowsIOException() throws Exception {
        // 既非 zip（docx）也非 OLE2（doc）的乱码内容
        byte[] garbage = "this is not a real doc file".getBytes(StandardCharsets.UTF_8);
        Files.write(tempDir.resolve("broken.doc"), garbage);
        MockMultipartFile file = new MockMultipartFile("file", "broken.doc", "application/msword", garbage);

        assertThatThrownBy(() -> DocumentParser.parseDocument(file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("文档解析失败");
    }

    @Test
    // xlsx 文件：包含 Sheet 标题与字符串/数字/布尔单元格
    void parsesExcelFile() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("案例数据");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("冒充客服");
            row.createCell(1).setCellValue(18);
            row.createCell(2).setCellValue(true);
            workbook.write(baos);
        }
        byte[] bytes = baos.toByteArray();
        Files.write(tempDir.resolve("cases.xlsx"), bytes);
        MockMultipartFile file = new MockMultipartFile("file", "cases.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        Map<String, Object> result = DocumentParser.parseDocument(file);

        assertThat(result.get("documentType")).isEqualTo("excel");
        String content = (String) result.get("content");
        assertThat(content).contains("【Sheet: 案例数据】")
                .contains("冒充客服")
                .contains("18.0")
                .contains("true");
    }

    // ---------- 类型识别与异常路径 ----------

    @Test
    // contentType 为 null 时按扩展名识别为 pdf
    void detectsTypeByExtensionWhenContentTypeMissing() throws Exception {
        Path pdfPath = tempDir.resolve("byext.pdf");
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.save(pdfPath.toFile());
        }
        MockMultipartFile file = new MockMultipartFile(
                "file", "byext.pdf", null, Files.readAllBytes(pdfPath));

        Map<String, Object> result = DocumentParser.parseDocument(file);

        assertThat(result.get("documentType")).isEqualTo("pdf");
    }

    @Test
    // 不支持的格式：documentType 为 unknown，走 Tika 兜底分支
    void unknownTypeFallsBackToTika() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "data.bin",
                "application/octet-stream", "some binary payload".getBytes(StandardCharsets.UTF_8));

        Map<String, Object> result = DocumentParser.parseDocument(file);

        assertThat(result.get("documentType")).isEqualTo("unknown");
        // 仅有 tika-core 时无注册的 Parser，兜底返回内容不为 null 即可
        assertThat(result.get("content")).isNotNull();
    }

    @Test
    // 损坏的 PDF：解析失败统一包装为 IOException
    void corruptPdfThrowsIOException() {
        MockMultipartFile file = new MockMultipartFile("file", "broken.pdf",
                "application/pdf", "this is not a real pdf".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> DocumentParser.parseDocument(file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("文档解析失败");
    }

    @Test
    // 损坏的 Word：XWPF 与 HWPF 均失败，抛出 IOException
    void corruptWordThrowsIOException() {
        MockMultipartFile file = new MockMultipartFile("file", "broken.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "not a word file".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> DocumentParser.parseDocument(file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("文档解析失败");
    }

    @Test
    // 文件名为 null 时不抛 NPE，按 contentType 识别
    void nullFileNameFallsBackToContentType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", null, "text/plain", "hello".getBytes(StandardCharsets.UTF_8));

        Map<String, Object> result = DocumentParser.parseDocument(file);

        assertThat(result.get("documentType")).isEqualTo("text");
        assertThat(result.get("content")).isEqualTo("hello");
    }

    // ---------- extractTitle ----------

    @Test
    // extractTitle：null 或空文件名返回默认标题
    void extractTitleReturnsDefaultForNullOrEmpty() {
        assertThat(DocumentParser.extractTitle(null)).isEqualTo("未命名文档");
        assertThat(DocumentParser.extractTitle("")).isEqualTo("未命名文档");
    }

    @Test
    // extractTitle：去除扩展名并将 ._- 替换为空格
    void extractTitleStripsExtensionAndSeparators() {
        assertThat(DocumentParser.extractTitle("防范_技巧.v2-final.md")).isEqualTo("防范 技巧 v2 final");
        assertThat(DocumentParser.extractTitle("README")).isEqualTo("README");
    }

    @Test
    // extractTitle：超长标题截断到 100 字符；仅剩符号时返回默认标题
    void extractTitleTruncatesAndHandlesSymbolOnlyName() {
        String longName = "a".repeat(120) + ".txt";
        assertThat(DocumentParser.extractTitle(longName)).hasSize(100);
        assertThat(DocumentParser.extractTitle("....txt")).isEqualTo("未命名文档");
    }

    // ---------- suggestCategory ----------

    @Test
    // suggestCategory：文件名或内容命中案例关键词归为"诈骗类型"
    void suggestCategoryDetectsCases() {
        assertThat(DocumentParser.suggestCategory("2024案例汇编.pdf", "")).isEqualTo("诈骗类型");
        assertThat(DocumentParser.suggestCategory("a.txt", "本文收录多个诈骗案例")).isEqualTo("诈骗类型");
    }

    @Test
    // suggestCategory：技巧/防范关键词归为"防范技巧"
    void suggestCategoryDetectsTips() {
        assertThat(DocumentParser.suggestCategory("防骗tip.txt", "")).isEqualTo("防范技巧");
        assertThat(DocumentParser.suggestCategory("a.txt", "常见防范技巧如下")).isEqualTo("防范技巧");
    }

    @Test
    // suggestCategory：指南/应对关键词归为"应对方法"
    void suggestCategoryDetectsGuides() {
        assertThat(DocumentParser.suggestCategory("应急指南.md", "")).isEqualTo("应对方法");
        assertThat(DocumentParser.suggestCategory("a.txt", "受骗后的应对方法")).isEqualTo("应对方法");
    }

    @Test
    // suggestCategory：无命中时归为"文档资料"，null 参数安全
    void suggestCategoryDefaultsToDocuments() {
        assertThat(DocumentParser.suggestCategory("random.txt", "随便一段内容")).isEqualTo("文档资料");
        assertThat(DocumentParser.suggestCategory(null, null)).isEqualTo("文档资料");
    }
}
