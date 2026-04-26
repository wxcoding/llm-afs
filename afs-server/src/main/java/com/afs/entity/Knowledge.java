package com.afs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 知识库实体类
 *
 * 对应数据库中的 knowledge 表
 *
 * 存储防诈骗知识库内容，包括防范技巧、诈骗类型、应对方法等，用于 RAG 知识库检索。
 *
 * 字段说明：
 * - id: 知识条目唯一标识，自增主键
 * - title: 知识标题，如"防范电信诈骗的十个凡是"
 * - category: 知识分类，如"防范技巧"、"诈骗类型"、"应对方法"
 * - content: 知识正文内容
 * - createTime: 知识添加时间
 */
@Data
@TableName("knowledge")
public class Knowledge {

    /** 知识条目唯一标识，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 知识标题，如"防范电信诈骗的十个凡是" */
    private String title;

    /** 知识分类，如"防范技巧"、"诈骗类型"、"应对方法" */
    private String category;

    /** 知识正文内容 */
    private String content;

    /** 知识添加时间 */
    private LocalDateTime createTime;
}
