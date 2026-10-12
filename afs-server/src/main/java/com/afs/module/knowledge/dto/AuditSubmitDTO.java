package com.afs.module.knowledge.dto;

import lombok.Data;

/**
 * 提交知识审核请求
 */
@Data
public class AuditSubmitDTO {

    /**
     * 知识条目 ID，为空表示新增知识
     */
    private Long knowledgeId;

    private String title;

    private String content;

    private String category;

    private Long submitUserId;
}
