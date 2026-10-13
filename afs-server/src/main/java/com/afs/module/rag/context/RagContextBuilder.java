package com.afs.module.rag.context;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 将检索来源转换为可注入提示词的上下文文本。
 */
@Component
public class RagContextBuilder {

    public String buildContext(List<Map<String, Object>> sources) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < sources.size(); i++) {
            Map<String, Object> source = sources.get(i);
            String content = String.valueOf(source.getOrDefault("content", ""));
            String title = String.valueOf(source.getOrDefault("title", ""));
            String sourceType = String.valueOf(source.getOrDefault("source", "未知来源"));

            if (i > 0) {
                context.append("\n\n");
            }
            context.append("【").append(sourceType);
            if (!title.isEmpty() && !"null".equals(title)) {
                context.append(" - ").append(title);
            }
            context.append("】\n").append(content);
        }
        return context.toString();
    }
}
