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

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DocumentParser {

    private static final Set<String> PDF_TYPES = Set.of("application/pdf", "application/x-pdf");
    private static final Set<String> WORD_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword"
    );
    private static final Set<String> MARKDOWN_TYPES = Set.of("text/markdown", "text/x-markdown");
    private static final Set<String> TEXT_TYPES = Set.of("text/plain", "text/html");
    private static final Set<String> EXCEL_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel"
    );

    private static final Parser MARKDOWN_PARSER = Parser.builder().build();
    private static final TextContentRenderer MARKDOWN_RENDERER = TextContentRenderer.builder().build();

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

    private static String parsePdf(InputStream inputStream) throws IOException {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

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

    private static String parseMarkdown(InputStream inputStream) throws IOException {
        String markdownContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        var document = MARKDOWN_PARSER.parse(markdownContent);
        return MARKDOWN_RENDERER.render(document);
    }

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

    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private static String parseText(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String parseWithTika(InputStream inputStream) throws IOException, TikaException {
        Tika tika = new Tika();
        return tika.parseToString(inputStream);
    }

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