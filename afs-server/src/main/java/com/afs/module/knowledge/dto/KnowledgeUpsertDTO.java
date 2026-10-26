package com.afs.module.knowledge.dto;

import lombok.Data;

/**
 * 新增/更新知识条目请求
 *
 * @author guanwx
 */
@Data
public class KnowledgeUpsertDTO {

    /** 知识标题 */
    private String title;

    /** 知识分类 */
    private String category;

    /** 知识内容 */
    private String content;
}
