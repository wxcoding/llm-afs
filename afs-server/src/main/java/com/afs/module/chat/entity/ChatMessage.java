package com.afs.module.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天消息实体类
 *
 * 对应数据表 chat_message，存储用户与 AI 的单条对话消息
 *
 * @author guanwx
 */
@Data
@TableName("chat_message")
public class ChatMessage {

    /** 消息 ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属会话 ID */
    private Long sessionId;

    /** 消息角色（user 用户 / assistant AI） */
    private String role;

    /** 消息内容 */
    private String content;

    /** RAG 检索来源，JSON 字符串，可为空 */
    private String sources;

    /** 消息创建时间 */
    private LocalDateTime createTime;
}
