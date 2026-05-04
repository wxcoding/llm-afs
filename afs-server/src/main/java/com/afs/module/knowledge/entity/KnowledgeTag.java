package com.afs.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("knowledge_tag")
public class KnowledgeTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String color;
    private String description;
    private Integer knowledgeCount;
    private LocalDateTime createTime;
}
