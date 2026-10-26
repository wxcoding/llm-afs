package com.afs.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("knowledge")
public class Knowledge {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String category;

    private String content;

    private String documentType;

    private String sourceFile;

    private Long fileSize;

    private Integer charCount;

    private LocalDateTime updateTime;

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
