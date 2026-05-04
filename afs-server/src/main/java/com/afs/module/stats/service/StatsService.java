package com.afs.module.stats.service;

import com.afs.module.chat.mapper.ChatMessageMapper;
import com.afs.module.chat.mapper.ChatSessionMapper;
import com.afs.module.knowledge.mapper.KnowledgeMapper;
import com.afs.module.scamcase.mapper.ScamCaseMapper;
import com.afs.module.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 统计服务类
 * 
 * 负责系统各模块数据统计功能
 */
@Service
public class StatsService {

    @Autowired
    private KnowledgeMapper knowledgeMapper;

    @Autowired
    private ScamCaseMapper scamCaseMapper;

    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private UserMapper userMapper;

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
