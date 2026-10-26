package com.afs.module.chat.dto;

import lombok.Data;

/**
 * 发送聊天消息请求
 *
 * @author guanwx
 */
@Data
public class SendMessageDTO {

    /** 用户 ID */
    private Long userId;

    /**
     * 会话 ID，为空表示新建会话
     */
    private Long sessionId;

    /** 消息内容 */
    private String content;
}
