package com.afs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天会话实体类
 *
 * 对应数据库中的 chat_session 表
 *
 * 用于组织用户的对话上下文，一个用户可以拥有多个会话，每个会话包含多条聊天消息。
 *
 * 字段说明：
 * - id: 会话唯一标识，自增主键
 * - userId: 所属用户 ID，关联 user 表
 * - title: 会话标题，通常取用户第一条消息的前20个字符
 * - createTime: 会话创建时间
 */
@Data
@TableName("chat_session")
public class ChatSession {

    /** 会话唯一标识，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 ID，关联 user 表的 id 字段 */
    private Long userId;

    /** 会话标题，通常取用户第一条消息的前20个字符 */
    private String title;

    /** 会话创建时间 */
    private LocalDateTime createTime;
}
