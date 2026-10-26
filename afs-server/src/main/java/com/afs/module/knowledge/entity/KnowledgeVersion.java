package com.afs.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 知识库版本记录实体，对应 knowledge_version 表，用于版本历史与回滚
 *
 * @author guanwx
 */
@Data
@TableName("knowledge_version")
public class KnowledgeVersion {
    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联的知识条目 ID */
    private Long knowledgeId;
    /** 该版本的标题 */
    private String title;
    /** 该版本的内容 */
    private String content;
    /** 变更摘要 */
    private String changeSummary;
    /** 操作人用户 ID */
    private Long operatorId;
    /** 创建时间 */
    private LocalDateTime createTime;
}
