package com.afs.module.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.afs.module.chat.entity.ChatSession;

/**
 * 聊天会话数据访问接口
 *
 * 提供 chat_session 表的增删改查能力，继承 MyBatis-Plus 通用 Mapper
 *
 * @author guanwx
 */
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
