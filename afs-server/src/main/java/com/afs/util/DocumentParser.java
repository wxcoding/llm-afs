package com.afs.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 文档解析工具类
 * 支持解析 PDF、Word、Markdown、Excel、Text 等常见文档格式
 * 用于从上传的文档中提取文本内容，存入向量数据库
 */
public class DocumentParser {

    /** PDF 文件 MIME 类型集合 */
    private static final Set<String> PDF_TYPES = Set.of("application/pdf", "application/x-pdf");

    /** Word 文档 MIME 类型集合（包含 .docx 和 .doc） */
    private static final Set<String> WORD_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword"
    );

    /** Markdown 文件 MIME 类型集合 */
    private static final Set<String> MARKDOWN_TYPES = Set.of("text/markdown", "text/x-markdown");

    /** 纯文本 MIME 类型集合 */
    private static final Set<String> TEXT_TYPES = Set.of("text/plain", "text/html");

    /** Excel 文件 MIME 类型集合 */
    private static final Set<String> EXCEL_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel"
    );

    /** Markdown 解析器实例（线程安全，可复用） */
    private static final Parser MARKDOWN_PARSER = Parser.builder().build();

    /** Markdown 渲染器实例（将 Markdown 转换为纯文本） */
    private static final TextContentRenderer MARKDOWN_RENDERER = TextContentRenderer.builder().build();

    /**
     * 解析上传的文档文件
     * 自动检测文档类型并提取文本内容
     *
     * @param file MultipartFile 上传的文件对象
     * @return Map 包含 fileName、fileSize、content、documentType、charCount
     * @throws IOException 文档解析失败时抛出
     */
    public static Map<String, Object> parseDocument(MultipartFile file) throws IOException {
        Map<String, Object> result = new HashMap<>();
        result.put("fileName", file.getOriginalFilename());
        result.put("fileSize", file.getSize());

        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";

        String content;
        String documentType = detectDocumentType(contentType, fileName);

        try {
            switch (documentType) {
                case "pdf" -> content = parsePdf(file.getInputStream());
                case "word" -> content = parseWord(file.getInputStream());
                case "markdown" -> content = parseMarkdown(file.getInputStream());
                case "excel" -> content = parseExcel(file.getInputStream());
                case "text" -> content = parseText(file.getInputStream());
                default -> content = parseWithTika(file.getInputStream());
            }
        } catch (Exception e) {
            throw new IOException("文档解析失败: " + e.getMessage(), e);
        }

        result.put("content", content);
        result.put("documentType", documentType);
        result.put("charCount", content.length());

        return result;
    }

    /**
     * 检测文档类型
     * 优先根据 MIME 类型判断，其次根据文件扩展名判断
     *
     * @param contentType 文件的 MIME 类型
     * @param fileName 文件名
     * @return 文档类型：pdf、word、markdown、excel、text、unknown
     */
    private static String detectDocumentType(String contentType, String fileName) {
        if (contentType != null) contentType = contentType.toLowerCase();
        if (fileName == null) fileName = "";

        if (PDF_TYPES.contains(contentType) || fileName.endsWith(".pdf")) {
            return "pdf";
        }
        if (WORD_TYPES.contains(contentType) || fileName.endsWith(".docx") || fileName.endsWith(".doc")) {
            return "word";
        }
        if (MARKDOWN_TYPES.contains(contentType) || fileName.endsWith(".md") || fileName.endsWith(".markdown")) {
            return "markdown";
        }
        if (EXCEL_TYPES.contains(contentType) || fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            return "excel";
        }
        if (TEXT_TYPES.contains(contentType) || fileName.endsWith(".txt") || fileName.endsWith(".html")) {
            return "text";
        }
        return "unknown";
    }

    /**
     * 解析 PDF 文档
     * 使用 PDFBox 库提取文本内容
     *
     * @param inputStream 文件输入流
     * @return 提取的文本内容
     * @throws IOException 读取或解析失败
     */
    private static String parsePdf(InputStream inputStream) throws IOException {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    /**
     * 解析 Word 文档
     * 支持 .docx 和 .doc 两种格式
     * 优先尝试解析 .docx 格式，失败则尝试 .doc 格式
     *
     * @param inputStream 文件输入流
     * @return 提取的文本内容
     * @throws IOException 解析失败
     */
    private static String parseWord(InputStream inputStream) throws IOException {
        byte[] content = inputStream.readAllBytes();

        try (ByteArrayInputStream bis = new ByteArrayInputStream(content);
             XWPFDocument document = new XWPFDocument(bis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        } catch (Exception e) {
            try (ByteArrayInputStream bis = new ByteArrayInputStream(content);
                 HWPFDocument doc = new HWPFDocument(bis);
                 WordExtractor extractor = new WordExtractor(doc)) {
                return extractor.getText();
            } catch (Exception ex) {
                throw new IOException("无法解析Word文档: " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * 解析 Markdown 文档
     * 将 Markdown 格式转换为纯文本
     *
     * @param inputStream 文件输入流
     * @return 转换后的纯文本内容
     * @throws IOException 读取失败
     */
    private static String parseMarkdown(InputStream inputStream) throws IOException {
        String markdownContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        var document = MARKDOWN_PARSER.parse(markdownContent);
        return MARKDOWN_RENDERER.render(document);
    }

    /**
     * 解析 Excel 文档
     * 遍历所有 Sheet 和单元格，提取文本内容
     *
     * @param inputStream 文件输入流
     * @return 提取的文本内容，格式为【Sheet名称】单元格内容...
     * @throws IOException 读取或解析失败
     */
    private static String parseExcel(InputStream inputStream) throws IOException {
        StringBuilder content = new StringBuilder();
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                var sheet = workbook.getSheetAt(i);
                content.append("【Sheet: ").append(sheet.getSheetName()).append("】\n");
                var rows = sheet.rowIterator();
                while (rows.hasNext()) {
                    var row = rows.next();
                    var cells = row.cellIterator();
                    while (cells.hasNext()) {
                        var cell = cells.next();
                        content.append(getCellValue(cell)).append("\t");
                    }
                    content.append("\n");
                }
                content.append("\n");
            }
        }
        return content.toString();
    }

    /**
     * 获取单元格的值
     * 根据单元格类型返回对应的字符串值
     *
     * @param cell 单元格对象
     * @return 单元格的字符串值
     */
    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    /**
     * 解析纯文本文件
     * 直接读取文件内容
     *
     * @param inputStream 文件输入流
     * @return 文件文本内容
     * @throws IOException 读取失败
     */
    private static String parseText(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * 使用 Tika 解析未知格式的文档
     * Tika 可以自动检测并解析多种文档格式
     *
     * @param inputStream 文件输入流
     * @return 提取的文本内容
     * @throws IOException 读取失败
     * @throws TikaException Tika 解析失败
     */
    private static String parseWithTika(InputStream inputStream) throws IOException, TikaException {
        Tika tika = new Tika();
        return tika.parseToString(inputStream);
    }

    /**
     * 从文件名提取文档标题
     * 去除文件扩展名和特殊字符，生成可读的标题
     *
     * @param fileName 原始文件名
     * @return 处理后的标题，默认为"未命名文档"
     */
    public static String extractTitle(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "未命名文档";
        }
        String name = fileName;
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0) {
            name = name.substring(0, dotIndex);
        }
        name = name.replaceAll("[._-]+", " ").trim();
        if (name.length() > 100) {
            name = name.substring(0, 100);
        }
        return name.isEmpty() ? "未命名文档" : name;
    }

    /**
     * 根据文件名和内容建议文档分类
     * 用于向量存储时的分类标注
     *
     * @param fileName 文件名
     * @param content 文档内容（用于辅助判断）
     * @return 建议的分类：诈骗类型、防范技巧、应对方法、文档资料
     */
    public static String suggestCategory(String fileName, String content) {
        if (fileName == null) fileName = "";
        String lowerFileName = fileName.toLowerCase();
        String lowerContent = content != null ? content.toLowerCase() : "";

        if (lowerFileName.contains("case") || lowerFileName.contains("案例") ||
                lowerContent.contains("诈骗案例") || lowerContent.contains("典型案例")) {
            return "诈骗类型";
        }
        if (lowerFileName.contains("tip") || lowerFileName.contains("技巧") ||
                lowerFileName.contains("防范") || lowerContent.contains("防范技巧")) {
            return "防范技巧";
        }
        if (lowerFileName.contains("guide") || lowerFileName.contains("指南") ||
                lowerFileName.contains("应对") || lowerContent.contains("应对方法")) {
            return "应对方法";
        }
        return "文档资料";
    }
}