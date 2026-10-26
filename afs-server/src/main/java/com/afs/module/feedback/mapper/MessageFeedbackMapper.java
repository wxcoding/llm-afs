package com.afs.module.feedback.mapper;

import com.afs.module.feedback.entity.MessageFeedback;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息反馈数据访问接口
 *
 * 提供 message_feedback 表的增删改查能力，继承 MyBatis-Plus 通用 Mapper
 *
 * @author guanwx
 */
@Mapper
public interface MessageFeedbackMapper extends BaseMapper<MessageFeedback> {
}
