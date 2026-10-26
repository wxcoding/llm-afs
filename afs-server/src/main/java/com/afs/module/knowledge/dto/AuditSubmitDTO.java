package com.afs.module.knowledge.dto;

import lombok.Data;

/**
 * 提交知识审核请求
 *
 * @author guanwx
 */
@Data
public class AuditSubmitDTO {

    /**
     * 知识条目 ID，为空表示新增知识
     */
    private Long knowledgeId;

    /** 知识标题 */
    private String title;

    /** 知识内容 */
    private String content;

    /** 知识分类 */
    private String category;

    /** 提交人用户 ID */
    private Long submitUserId;
}
