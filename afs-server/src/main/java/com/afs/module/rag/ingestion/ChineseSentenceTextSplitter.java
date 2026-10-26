package com.afs.module.rag.ingestion;

import org.springframework.ai.transformer.splitter.TextSplitter;

import java.util.ArrayList;
import java.util.List;

/**
 * 面向中文文本的句子级切分器。
 *
 * 与 Spring AI 默认 TokenTextSplitter 的差异：
 * 1. 以中文句读（。！？；）等标点作为切分锚点，不会在句子中间硬切；
 * 2. 相邻块之间保留固定长度的重叠内容，缓解块边界的上下文丢失；
 * 3. 按字符数控制块长。中文场景下 1 字 ≈ 1 token（cl100k），与 token 计量近似等价。
 * @author guanwx
 */
public class ChineseSentenceTextSplitter extends TextSplitter {

    /** 句子结束符：中文全角标点 + 对应半角标点 + 换行 */
    private static final String SENTENCE_ENDINGS = "。！？；!?;\n";

    /** 单个块的最大字符数 */
    private final int maxChunkSize;

    /** 相邻块之间重叠的字符数 */
    private final int overlap;

    /** 低于该长度的碎块会被并入前一块，避免产生无意义的短向量 */
    private final int minChunkSize;

    /**
     * 使用默认参数创建切分器（块长 400 字、重叠 60 字、碎块阈值 20 字）。
     */
    public ChineseSentenceTextSplitter() {
        this(400, 60, 20);
    }

    /**
     * 创建切分器并指定切分参数。
     *
     * @param maxChunkSize 单个块的最大字符数，必须大于 0
     * @param overlap      相邻块之间重叠的字符数，必须满足 0 <= overlap < maxChunkSize
     * @param minChunkSize 碎块阈值，低于该长度的块并入前一块，不能为负数
     * @throws IllegalArgumentException 参数不满足约束时抛出
     */
    public ChineseSentenceTextSplitter(int maxChunkSize, int overlap, int minChunkSize) {
        if (maxChunkSize <= 0) {
            throw new IllegalArgumentException("maxChunkSize 必须大于 0");
        }
        // 重叠必须小于块长，否则滑动步长 <= 0，长句硬切会死循环
        if (overlap < 0 || overlap >= maxChunkSize) {
            throw new IllegalArgumentException("overlap 必须满足 0 <= overlap < maxChunkSize");
        }
        if (minChunkSize < 0) {
            throw new IllegalArgumentException("minChunkSize 不能为负数");
        }
        this.maxChunkSize = maxChunkSize;
        this.overlap = overlap;
        this.minChunkSize = minChunkSize;
    }

    /**
     * 将文本按句子边界贪心切分为块，相邻块保留重叠内容。
     *
     * @param text 待切分文本，为空时返回空列表
     * @return 切分后的文本块列表
     */
    @Override
    public List<String> splitText(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        StringBuilder current = new StringBuilder();
        for (String sentence : splitSentences(text)) {
            // 单句本身就超过块长上限时，只能按长度硬切，保证任何输入都能收敛
            if (sentence.length() > maxChunkSize) {
                flushChunk(current, chunks);
                splitLongSentence(sentence, chunks);
                continue;
            }
            // 当前块放不下这句时先封存，并把上一块末尾的重叠内容带入新块
            if (current.length() + sentence.length() > maxChunkSize && current.length() > 0) {
                flushChunk(current, chunks);
                current.append(buildOverlap(chunks.get(chunks.size() - 1)));
            }
            current.append(sentence);
        }
        flushChunk(current, chunks);
        return chunks;
    }

    /**
     * 按句子结束符切分，结束符保留在句尾。
     */
    private List<String> splitSentences(String text) {
        List<String> sentences = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            current.append(c);
            if (SENTENCE_ENDINGS.indexOf(c) >= 0) {
                sentences.add(current.toString());
                current.setLength(0);
            }
        }
        if (current.length() > 0) {
            sentences.add(current.toString());
        }
        return sentences;
    }

    /**
     * 将超长单句按 maxChunkSize 硬切，块间以 (maxChunkSize - overlap) 的步长滑动。
     */
    private void splitLongSentence(String sentence, List<String> chunks) {
        int step = maxChunkSize - overlap;
        int start = 0;
        while (start < sentence.length()) {
            int end = Math.min(start + maxChunkSize, sentence.length());
            String piece = sentence.substring(start, end).trim();
            if (!piece.isEmpty()) {
                chunks.add(piece);
            }
            if (end == sentence.length()) {
                break;
            }
            start += step;
        }
    }

    /**
     * 取上一块末尾不超过 overlap 字符的内容作为新块开头；尽量对齐到句子边界，避免以半句话开头。
     */
    private String buildOverlap(String previousChunk) {
        if (overlap == 0 || previousChunk == null || previousChunk.isEmpty()) {
            return "";
        }
        int from = Math.max(0, previousChunk.length() - overlap);
        // 向后扫描到最近的句子结束符之后，保证重叠部分从一句完整的话开始
        while (from < previousChunk.length() && !isSentenceEnding(previousChunk.charAt(from - 1))) {
            from++;
        }
        return previousChunk.substring(Math.min(from, previousChunk.length()));
    }

    /**
     * 判断字符是否为句子结束符。
     *
     * @param c 待判断字符
     * @return 是句子结束符时返回 true
     */
    private boolean isSentenceEnding(char c) {
        return SENTENCE_ENDINGS.indexOf(c) >= 0;
    }

    /**
     * 封存当前块：去空白、跳过空块，过短的碎块直接并入上一块。
     */
    private void flushChunk(StringBuilder current, List<String> chunks) {
        String chunk = current.toString().trim();
        current.setLength(0);
        if (chunk.isEmpty()) {
            return;
        }
        if (chunk.length() < minChunkSize && !chunks.isEmpty()) {
            // 并入前一块可能使其略微超过 maxChunkSize，可接受
            chunks.set(chunks.size() - 1, chunks.get(chunks.size() - 1) + chunk);
            return;
        }
        chunks.add(chunk);
    }
}
