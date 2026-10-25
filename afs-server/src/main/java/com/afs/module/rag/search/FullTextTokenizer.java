package com.afs.module.rag.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PostgreSQL simple 配置全文检索使用的确定性分词器。
 * 连续中文按重叠二元词切分，其他字母和数字序列统一转成小写词项。
 */
public final class FullTextTokenizer {

    private static final Pattern TOKEN_PATTERN =
            Pattern.compile("\\p{IsHan}+|[\\p{L}\\p{N}]+");

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
