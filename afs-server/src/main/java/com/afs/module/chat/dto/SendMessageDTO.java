package com.afs.module.chat.dto;

import lombok.Data;

/**
 * 发送聊天消息请求
 */
@Data
public class SendMessageDTO {

    private Long userId;

    /**
     * 会话 ID，为空表示新建会话
     */
    private Long sessionId;

    private String content;
}
