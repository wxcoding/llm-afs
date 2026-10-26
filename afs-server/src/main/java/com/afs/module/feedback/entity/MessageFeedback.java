package com.afs.module.feedback.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 消息反馈实体类
 *
 * 对应数据表 message_feedback，存储用户对 AI 回复消息的评分与反馈信息
 *
 * @author guanwx
 */
@Data
@TableName("message_feedback")
public class MessageFeedback {
    /** 反馈记录 ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 被反馈的消息 ID */
    private Long messageId;
    /** 提交反馈的用户 ID */
    private Long userId;
    /** 评分（1-5星） */
    private Integer rating;
    /** 是否有帮助 */
    private Boolean isHelpful;
    /** 反馈类型 */
    private String feedbackType;
    /** 反馈内容 */
    private String feedbackContent;
    /** 反馈创建时间 */
    private LocalDateTime createTime;
}
