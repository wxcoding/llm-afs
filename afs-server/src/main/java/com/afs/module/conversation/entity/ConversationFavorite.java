package com.afs.module.conversation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("conversation_favorite")
public class ConversationFavorite {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long messageId;
    private String title;
    private LocalDateTime createTime;
}
