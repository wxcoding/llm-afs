package com.afs.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 知识库审核记录实体，对应 knowledge_audit 表，记录内容变更的审核申请与结果
 *
 * @author guanwx
 */
@Data
@TableName("knowledge_audit")
public class KnowledgeAudit {
    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联的知识条目 ID */
    private Long knowledgeId;
    /** 提交审核的标题 */
    private String title;
    /** 提交审核的内容 */
    private String content;
    /** 提交审核的分类 */
    private String category;
    /** 审核状态：pending-待审核，approved-已通过，rejected-已拒绝 */
    private String auditStatus;
    /** 审核意见 */
    private String auditComment;
    /** 提交人用户 ID */
    private Long submitUserId;
    /** 审核人用户 ID */
    private Long auditUserId;
    /** 提交时间 */
    private LocalDateTime submitTime;
    /** 审核时间 */
    private LocalDateTime auditTime;
}
