package com.afs.module.stats.service;

import com.afs.module.chat.mapper.ChatMessageMapper;
import com.afs.module.chat.mapper.ChatSessionMapper;
import com.afs.module.knowledge.mapper.KnowledgeMapper;
import com.afs.module.scamcase.mapper.ScamCaseMapper;
import com.afs.module.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * StatsService 单元测试
 *
 * 覆盖系统统计口径：知识、案例、会话、消息、用户五类计数
 * 分别来自五个 Mapper 的 selectCount(null) 全表计数。
 */
@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private KnowledgeMapper knowledgeMapper;

    @Mock
    private ScamCaseMapper scamCaseMapper;

    @Mock
    private ChatSessionMapper chatSessionMapper;

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private StatsService statsService;

    // 统计结果包含五个模块的全表计数，且逐一对应到正确的 Mapper
    @Test
    void getStatsAggregatesAllModuleCounts() {
        when(knowledgeMapper.selectCount(isNull())).thenReturn(10L);
        when(scamCaseMapper.selectCount(isNull())).thenReturn(20L);
        when(chatSessionMapper.selectCount(isNull())).thenReturn(30L);
        when(chatMessageMapper.selectCount(isNull())).thenReturn(40L);
        when(userMapper.selectCount(isNull())).thenReturn(50L);

        Map<String, Object> stats = statsService.getStats();

        assertEquals(10L, stats.get("knowledgeCount"));
        assertEquals(20L, stats.get("caseCount"));
        assertEquals(30L, stats.get("sessionCount"));
        assertEquals(40L, stats.get("messageCount"));
        assertEquals(50L, stats.get("userCount"));
        // 校验五个 Mapper 均以 null Wrapper 做全表计数
        verify(knowledgeMapper).selectCount(isNull());
        verify(scamCaseMapper).selectCount(isNull());
        verify(chatSessionMapper).selectCount(isNull());
        verify(chatMessageMapper).selectCount(isNull());
        verify(userMapper).selectCount(isNull());
    }

    // 各模块计数为零时正常返回 0
    @Test
    void getStatsReturnsZerosWhenTablesEmpty() {
        when(knowledgeMapper.selectCount(isNull())).thenReturn(0L);
        when(scamCaseMapper.selectCount(isNull())).thenReturn(0L);
        when(chatSessionMapper.selectCount(isNull())).thenReturn(0L);
        when(chatMessageMapper.selectCount(isNull())).thenReturn(0L);
        when(userMapper.selectCount(isNull())).thenReturn(0L);

        Map<String, Object> stats = statsService.getStats();

        assertEquals(0L, stats.get("knowledgeCount"));
        assertEquals(0L, stats.get("userCount"));
        assertEquals(5, stats.size());
    }
}
