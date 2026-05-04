package com.afs.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("knowledge_version")
public class KnowledgeVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long knowledgeId;
    private String title;
    private String content;
    private String changeSummary;
    private Long operatorId;
    private LocalDateTime createTime;
}
