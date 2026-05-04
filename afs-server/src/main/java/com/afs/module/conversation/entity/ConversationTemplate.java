package com.afs.module.conversation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("conversation_template")
public class ConversationTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private String category;
    private Integer usageCount;
    private Boolean isPublic;
    private Long createUserId;
    private LocalDateTime createTime;
}
