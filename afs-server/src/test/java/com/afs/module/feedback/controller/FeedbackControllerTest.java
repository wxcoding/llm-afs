package com.afs.module.feedback.controller;

import com.afs.exception.GlobalExceptionHandler;
import com.afs.module.feedback.entity.MessageFeedback;
import com.afs.module.feedback.service.MessageFeedbackService;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FeedbackController 单元测试
 *
 * 基于 MockMvc standalone 模式验证消息反馈的提交与查询接口，
 * 包含缺少必需参数（userId）时的 400 响应。
 */
class FeedbackControllerTest {

    private MockMvc mockMvc;

    private MessageFeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        feedbackService = mock(MessageFeedbackService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new FeedbackController(feedbackService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // 提交反馈成功：全参数路径
    @Test
    void submitFeedbackReturnsSavedFeedback() throws Exception {
        MessageFeedback feedback = buildFeedback(9L, 100L, 1L);
        when(feedbackService.saveFeedback(100L, 1L, 5, true, "accurate", "很有帮助"))
                .thenReturn(feedback);

        mockMvc.perform(post("/api/feedback/message/100")
                        .param("userId", "1")
                        .param("rating", "5")
                        .param("isHelpful", "true")
                        .param("feedbackType", "accurate")
                        .param("feedbackContent", "很有帮助"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(9))
                .andExpect(jsonPath("$.data.rating").value(5));
    }

    // 提交反馈：可选参数缺省时以 null 透传给 service
    @Test
    void submitFeedbackAllowsMissingOptionalParams() throws Exception {
        MessageFeedback feedback = buildFeedback(null, 100L, 1L);
        when(feedbackService.saveFeedback(100L, 1L, null, null, null, null)).thenReturn(feedback);

        mockMvc.perform(post("/api/feedback/message/100").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messageId").value(100));
        verify(feedbackService).saveFeedback(100L, 1L, null, null, null, null);
    }

    // 提交反馈：缺少必需的 userId 参数时返回 400
    @Test
    void submitFeedbackReturns400WhenUserIdMissing() throws Exception {
        mockMvc.perform(post("/api/feedback/message/100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("缺少必需参数: userId"));
        verify(feedbackService, never()).saveFeedback(any(), any(), any(), any(), any(), any());
    }

    // 查询反馈：存在记录时返回
    @Test
    void getFeedbackReturnsRecord() throws Exception {
        MessageFeedback feedback = buildFeedback(9L, 100L, 1L);
        stubLambdaQuery(feedback);

        mockMvc.perform(get("/api/feedback/message/100").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(9));
    }

    // 查询反馈：无记录时 data 为 null
    @Test
    void getFeedbackReturnsNullDataWhenAbsent() throws Exception {
        stubLambdaQuery(null);

        mockMvc.perform(get("/api/feedback/message/100").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    // 打桩 controller 中 lambdaQuery().eq().eq().one() 链式调用
    private void stubLambdaQuery(MessageFeedback result) {
        LambdaQueryChainWrapper<MessageFeedback> wrapper = mock(LambdaQueryChainWrapper.class);
        when(feedbackService.lambdaQuery()).thenReturn(wrapper);
        when(wrapper.eq(ArgumentMatchers.<SFunction<MessageFeedback, ?>>any(), any()))
                .thenReturn(wrapper);
        when(wrapper.one()).thenReturn(result);
    }

    private MessageFeedback buildFeedback(Long id, Long messageId, Long userId) {
        MessageFeedback feedback = new MessageFeedback();
        feedback.setId(id);
        feedback.setMessageId(messageId);
        feedback.setUserId(userId);
        feedback.setRating(5);
        feedback.setIsHelpful(true);
        return feedback;
    }
}
