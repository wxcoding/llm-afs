package com.afs.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("knowledge_tag_relation")
public class KnowledgeTagRelation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long knowledgeId;
    private Long tagId;
    private LocalDateTime createTime;
}
