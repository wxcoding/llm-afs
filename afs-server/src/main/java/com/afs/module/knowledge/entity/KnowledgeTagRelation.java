package com.afs.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 知识库与标签的关联关系实体，对应 knowledge_tag_relation 表
 *
 * @author guanwx
 */
@Data
@TableName("knowledge_tag_relation")
public class KnowledgeTagRelation {
    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 知识条目 ID */
    private Long knowledgeId;
    /** 标签 ID */
    private Long tagId;
    /** 创建时间 */
    private LocalDateTime createTime;
}
