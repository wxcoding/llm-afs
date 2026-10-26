package com.afs.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 知识库条目实体，对应 knowledge 表
 *
 * @author guanwx
 */
@Data
@TableName("knowledge")
public class Knowledge {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 知识条目标题 */
    private String title;

    /** 知识分类 */
    private String category;

    /** 知识内容 */
    private String content;

    /** 文档类型（如 PDF、Word、Markdown 等） */
    private String documentType;

    /** 来源文件名 */
    private String sourceFile;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 内容字符数 */
    private Integer charCount;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /**
     * 状态: DRAFT-草稿, PENDING_REVIEW-待审核, ACTIVE-已发布, REJECTED-已拒绝
     */
    private String status;

    /**
     * 向量化处理状态: PENDING-待处理, PROCESSING-处理中, SUCCESS-已完成, FAILED-失败
     */
    private String vectorStatus;
}
