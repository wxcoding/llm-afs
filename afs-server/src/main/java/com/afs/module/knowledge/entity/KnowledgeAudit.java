package com.afs.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("knowledge_audit")
public class KnowledgeAudit {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long knowledgeId;
    private String title;
    private String content;
    private String category;
    private String auditStatus;
    private String auditComment;
    private Long submitUserId;
    private Long auditUserId;
    private LocalDateTime submitTime;
    private LocalDateTime auditTime;
}
