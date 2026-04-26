package com.afs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天消息实体类
 *
 * 对应数据库中的 chat_message 表
 *
 * 存储用户与 AI 助手的对话记录，包含角色、消息内容和来源信息。
 *
 * 字段说明：
 * - id: 消息唯一标识，自增主键
 * - sessionId: 所属会话 ID，关联 chat_session 表
 * - role: 消息角色，user 表示用户，assistant 表示 AI 助手
 * - content: 消息内容
 * - sources: 关联的参考资料（JSON 格式），包含从知识库检索到的相关内容
 * - createTime: 消息发送时间
 */
@Data
@TableName("chat_message")
public class ChatMessage {

    /** 消息唯一标识，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属会话 ID，关联 chat_session 表的 id 字段 */
    private Long sessionId;

    /** 消息角色：user=用户发送的消息，assistant=AI 助手回复 */
    private String role;

    /** 消息正文内容 */
    private String content;

    /** 关联的参考资料（JSON 格式），AI 回答时引用的知识库内容 */
    private String sources;

    /** 消息发送时间 */
    private LocalDateTime createTime;
}
