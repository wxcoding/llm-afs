package com.afs.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 知识库标签实体，对应 knowledge_tag 表
 *
 * @author guanwx
 */
@Data
@TableName("knowledge_tag")
public class KnowledgeTag {
    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 标签名称 */
    private String name;
    /** 标签颜色 */
    private String color;
    /** 标签描述 */
    private String description;
    /** 关联的知识条目数量 */
    private Integer knowledgeCount;
    /** 创建时间 */
    private LocalDateTime createTime;
}
