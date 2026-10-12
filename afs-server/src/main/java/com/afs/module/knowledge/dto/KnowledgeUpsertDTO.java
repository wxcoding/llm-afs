package com.afs.module.knowledge.dto;

import lombok.Data;

/**
 * 新增/更新知识条目请求
 */
@Data
public class KnowledgeUpsertDTO {

    private String title;

    private String category;

    private String content;
}
