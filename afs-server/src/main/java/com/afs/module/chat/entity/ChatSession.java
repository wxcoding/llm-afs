package com.afs.module.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天会话实体类
 *
 * 对应数据表 chat_session，存储用户的对话会话信息
 *
 * @author guanwx
 */
@Data
@TableName("chat_session")
public class ChatSession {

    /** 会话 ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 ID */
    private Long userId;

    /** 会话标题 */
    private String title;

    /** 会话创建时间 */
    private LocalDateTime createTime;
}
