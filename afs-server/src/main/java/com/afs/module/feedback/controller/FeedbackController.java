package com.afs.module.feedback.controller;

import com.afs.common.Result;
import com.afs.module.feedback.entity.MessageFeedback;
import com.afs.module.feedback.service.MessageFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户反馈控制器
 *
 * 提供 AI 回复消息的评分、反馈提交与查询接口
 *
 * @author guanwx
 */
@Tag(name = "用户反馈", description = "消息反馈、评分等接口")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/feedback")
@CrossOrigin
public class FeedbackController {

    /** 消息反馈服务 */
    private final MessageFeedbackService feedbackService;

    /**
     * 提交指定消息的用户反馈（已存在时更新）
     *
     * @param messageId       消息 ID
     * @param userId          用户 ID
     * @param rating          评分（1-5星），可为空
     * @param isHelpful       是否有帮助，可为空
     * @param feedbackType    反馈类型，可为空
     * @param feedbackContent 反馈内容，可为空
     * @return 保存后的反馈记录
     */
    @Operation(summary = "提交消息反馈", description = "对AI回复消息进行评分、标记是否有帮助、提交反馈内容")
    @PostMapping("/message/{messageId}")
    public Result<MessageFeedback> submitFeedback(
            @Parameter(description = "消息 ID", required = true) @PathVariable Long messageId,
            @Parameter(description = "用户 ID", required = true) @RequestParam Long userId,
            @Parameter(description = "评分（1-5星）") @RequestParam(required = false) Integer rating,
            @Parameter(description = "是否有帮助") @RequestParam(required = false) Boolean isHelpful,
            @Parameter(description = "反馈类型") @RequestParam(required = false) String feedbackType,
            @Parameter(description = "反馈内容") @RequestParam(required = false) String feedbackContent) {
        MessageFeedback feedback = feedbackService.saveFeedback(
                messageId, userId, rating, isHelpful, feedbackType, feedbackContent);
        return Result.success(feedback);
    }

    /**
     * 获取用户对指定消息的反馈记录
     *
     * @param messageId 消息 ID
     * @param userId    用户 ID
     * @return 反馈记录，不存在时返回空
     */
    @Operation(summary = "获取消息反馈", description = "获取用户对指定消息的反馈记录")
    @GetMapping("/message/{messageId}")
    public Result<MessageFeedback> getFeedback(
            @Parameter(description = "消息 ID", required = true) @PathVariable Long messageId,
            @Parameter(description = "用户 ID", required = true) @RequestParam Long userId) {
        return Result.success(feedbackService.lambdaQuery()
                .eq(MessageFeedback::getMessageId, messageId)
                .eq(MessageFeedback::getUserId, userId)
                .one());
    }
}
