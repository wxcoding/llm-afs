package com.afs.module.stats.service;

import com.afs.module.chat.mapper.ChatMessageMapper;
import com.afs.module.chat.mapper.ChatSessionMapper;
import com.afs.module.knowledge.mapper.KnowledgeMapper;
import com.afs.module.scamcase.mapper.ScamCaseMapper;
import com.afs.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 统计服务类
 * 
 * 负责系统各模块数据统计功能
 *
 * @author guanwx
 */
@RequiredArgsConstructor
@Service
public class StatsService {

    /** 知识库数据访问接口 */
    private final KnowledgeMapper knowledgeMapper;

    /** 诈骗案例数据访问接口 */
    private final ScamCaseMapper scamCaseMapper;

    /** 聊天会话数据访问接口 */
    private final ChatSessionMapper chatSessionMapper;

    /** 聊天消息数据访问接口 */
    private final ChatMessageMapper chatMessageMapper;

    /** 用户数据访问接口 */
    private final UserMapper userMapper;

    /**
     * 统计系统各模块数据表的记录总数
     *
     * @return 统计结果，包含知识、案例、会话、消息和用户数量
     */
    public Map<String, Object> getStats() {
        Map<String, Object> result = new HashMap<>();
        result.put("knowledgeCount", knowledgeMapper.selectCount(null));
        result.put("caseCount", scamCaseMapper.selectCount(null));
        result.put("sessionCount", chatSessionMapper.selectCount(null));
        result.put("messageCount", chatMessageMapper.selectCount(null));
        result.put("userCount", userMapper.selectCount(null));
        return result;
    }
}
