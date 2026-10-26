package com.afs.module.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.afs.module.chat.entity.ChatMessage;

/**
 * 聊天消息数据访问接口
 *
 * 提供 chat_message 表的增删改查能力，继承 MyBatis-Plus 通用 Mapper
 *
 * @author guanwx
 */
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
