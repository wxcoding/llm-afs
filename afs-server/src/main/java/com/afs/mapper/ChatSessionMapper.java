package com.afs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.afs.entity.ChatSession;

/**
 * 聊天会话数据访问层接口
 *
 * 继承 MyBatis-Plus 的 BaseMapper，提供 ChatSession 实体的基础 CRUD 操作。
 *
 * 支持的操作：
 * - selectById: 根据 ID 查询会话
 * - insert: 创建新会话
 * - updateById: 根据 ID 更新会话
 * - deleteById: 根据 ID 删除会话
 * - selectList: 查询用户的所有会话
 */
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
