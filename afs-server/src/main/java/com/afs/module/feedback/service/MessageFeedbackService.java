package com.afs.module.feedback.service;

import com.afs.module.feedback.entity.MessageFeedback;
import com.afs.module.feedback.mapper.MessageFeedbackMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 消息反馈服务类
 * 
 * 负责用户对AI回复消息的评分、反馈收集等业务逻辑
 */
@Service
public class MessageFeedbackService extends ServiceImpl<MessageFeedbackMapper, MessageFeedback> {

    public MessageFeedback saveFeedback(Long messageId, Long userId, Integer rating, Boolean isHelpful,
                                        String feedbackType, String feedbackContent) {
        QueryWrapper<MessageFeedback> wrapper = new QueryWrapper<>();
        wrapper.eq("message_id", messageId).eq("user_id", userId);
        MessageFeedback existing = this.getOne(wrapper);

        MessageFeedback feedback = new MessageFeedback();
        feedback.setMessageId(messageId);
        feedback.setUserId(userId);
        feedback.setRating(rating);
        feedback.setIsHelpful(isHelpful);
        feedback.setFeedbackType(feedbackType);
        feedback.setFeedbackContent(feedbackContent);

        if (existing != null) {
            feedback.setId(existing.getId());
            this.updateById(feedback);
        } else {
            this.save(feedback);
        }
        return feedback;
    }
}
