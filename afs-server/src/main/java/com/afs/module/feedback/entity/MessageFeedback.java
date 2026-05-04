package com.afs.module.feedback.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("message_feedback")
public class MessageFeedback {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long messageId;
    private Long userId;
    private Integer rating;
    private Boolean isHelpful;
    private String feedbackType;
    private String feedbackContent;
    private LocalDateTime createTime;
}
