package com.afs.module.feedback.service;

import com.afs.module.feedback.entity.MessageFeedback;
import com.afs.module.feedback.mapper.MessageFeedbackMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

/**
 * MessageFeedbackService 单元测试
 *
 * 覆盖消息反馈的"有则更新、无则新增"（upsert）逻辑。
 * 由于继承 MyBatis-Plus ServiceImpl，通过 ReflectionTestUtils 注入 mock 的 baseMapper；
 * 注意 3.5.5 版本 getOne(wrapper) 内部走 baseMapper.selectOne(wrapper, true)。
 */
class MessageFeedbackServiceTest {

    private MessageFeedbackMapper feedbackMapper;

    private MessageFeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        feedbackMapper = mock(MessageFeedbackMapper.class);
        feedbackService = new MessageFeedbackService();
        // ServiceImpl 的 baseMapper 由父类持有，需反射注入
        ReflectionTestUtils.setField(feedbackService, "baseMapper", feedbackMapper);
    }

    // 首次反馈：无历史记录时执行新增
    @Test
    void saveFeedbackInsertsWhenNoExistingRecord() {
        // getOne 内部调用 selectOne(wrapper, true)，返回 null 表示无历史反馈
        when(feedbackMapper.selectOne(any(), anyBoolean())).thenReturn(null);

        MessageFeedback result = feedbackService.saveFeedback(
                100L, 1L, 5, true, "accurate", "很有帮助");

        assertNull(result.getId());
        assertEquals(100L, result.getMessageId());
        assertEquals(1L, result.getUserId());
        assertEquals(5, result.getRating());
        assertTrue(result.getIsHelpful());
        verify(feedbackMapper).insert(any(MessageFeedback.class));
        verify(feedbackMapper, never()).updateById(any());
    }

    // 重复反馈：沿用原记录 ID 执行更新
    @Test
    void saveFeedbackUpdatesWhenRecordExists() {
        MessageFeedback existing = new MessageFeedback();
        existing.setId(9L);
        existing.setMessageId(100L);
        existing.setUserId(1L);
        when(feedbackMapper.selectOne(any(), anyBoolean())).thenReturn(existing);

        MessageFeedback result = feedbackService.saveFeedback(
                100L, 1L, 3, false, "inaccurate", "回答有误");

        // 沿用已有记录主键
        assertEquals(9L, result.getId());
        verify(feedbackMapper).updateById(any(MessageFeedback.class));
        verify(feedbackMapper, never()).insert(any(MessageFeedback.class));
    }

    // 可选字段为 null 时也能正常保存
    @Test
    void saveFeedbackAllowsNullOptionalFields() {
        when(feedbackMapper.selectOne(any(), anyBoolean())).thenReturn(null);

        MessageFeedback result = feedbackService.saveFeedback(100L, 1L, null, null, null, null);

        assertNull(result.getRating());
        assertNull(result.getIsHelpful());
        assertNull(result.getFeedbackType());
        assertNull(result.getFeedbackContent());
        verify(feedbackMapper).insert(any(MessageFeedback.class));
    }
}
