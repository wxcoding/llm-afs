package com.afs.module.rag.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 因默认 parser 不对中文分词，改为 Java 侧重叠二元切分配合 simple 配置，索引列用生成列维护，两路结果按名次做 RRF 融合
 *
 * PostgreSQL simple 配置全文检索使用的确定性分词器。
 * 连续中文按重叠二元词切分，其他字母和数字序列统一转成小写词项。
 *
 * @author guanwx
 */
public final class FullTextTokenizer {

    /**
     * 词项匹配模式（顺序敏感，从左到右优先匹配）：
     *
     * 1. {@code \p{IsHan}+}  — 一个或多个连续汉字（Unicode Script = HAN）。
     *    汉字同时属于 {@code \p{L}}（字母类），因此必须放在交替项的前面，
     *    才能确保整段连续汉字被整体捕获为一个 token，而非拆散后与相邻
     *    英文/数字混成一个 token。后续 {@link #addHanBigrams} 会将其切为
     *    重叠二元词（如"网络诈骗"→"网络","络诈","诈骗"）。
     *
     * 2. {@code |}            — 正则交替（OR）。
     *
     * 3. {@code [\p{L}\p{N}]+} — 一个或多个"非汉字字母"或"数字"字符。
     *    \p{L} 覆盖所有 Unicode 字母（含英文、日文假名等）；
     *    \p{N} 覆盖所有 Unicode 数字（含 ASCII 0-9、全角０－９等）。
     *    这类 token 仅做小写归一化，不再拆分。
     *
     * 整体效果：文本中除上述两类之外的字符（标点、空格、符号等）均被忽略。
     */
    private static final Pattern TOKEN_PATTERN =
            Pattern.compile("\\p{IsHan}+|[\\p{L}\\p{N}]+");

    /**
     * 工具类，禁止实例化。
     */
    private FullTextTokenizer() {
    }

    /**
     * 将文本转换为全文索引和查询共用的词项列表。
     */
    public static List<String> tokenize(String text) {
        List<String> terms = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return terms;
        }

        Matcher matcher = TOKEN_PATTERN.matcher(text);
        while (matcher.find()) {
            String token = matcher.group().toLowerCase(Locale.ROOT);
            if (isHanRun(token)) {
                addHanBigrams(token, terms);
            } else {
                terms.add(token);
            }
        }
        return terms;
    }

    /**
     * 判断一个词项是否完全由汉字组成。
     */
    private static boolean isHanRun(String token) {
        return token.codePoints().allMatch(
                codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }

    /**
     * 将连续汉字串切为重叠二元词；单字直接保留原样。
     */
    private static void addHanBigrams(String token, List<String> terms) {
        int[] codePoints = token.codePoints().toArray();
        if (codePoints.length == 1) {
            terms.add(token);
            return;
        }

        // 重叠二元词可使“网络诈骗”与包含“络诈”“诈骗”等片段的查询匹配。
        for (int i = 0; i < codePoints.length - 1; i++) {
            terms.add(new String(codePoints, i, 2));
        }
    }
}
