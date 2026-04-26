package com.afs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.afs.entity.ChatMessage;

/**
 * 聊天消息数据访问层接口
 *
 * 继承 MyBatis-Plus 的 BaseMapper，提供 ChatMessage 实体的基础 CRUD 操作。
 *
 * 支持的操作：
 * - selectById: 根据 ID 查询消息
 * - insert: 插入新消息
 * - updateById: 根据 ID 更新消息
 * - deleteById: 根据 ID 删除消息
 * - selectList: 查询会话的所有消息
 */
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
