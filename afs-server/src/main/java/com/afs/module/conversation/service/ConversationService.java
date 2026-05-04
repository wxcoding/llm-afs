package com.afs.module.conversation.service;

import com.afs.module.conversation.entity.ConversationFavorite;
import com.afs.module.conversation.entity.ConversationTemplate;
import com.afs.module.conversation.mapper.ConversationFavoriteMapper;
import com.afs.module.conversation.mapper.ConversationTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话服务类
 * 
 * 负责对话收藏管理、会话模板管理等业务逻辑
 */
@Service
public class ConversationService {

    @Autowired
    private ConversationFavoriteMapper favoriteMapper;
    @Autowired
    private ConversationTemplateMapper templateMapper;

    public ConversationFavorite addFavorite(Long userId, Long messageId, String title) {
        ConversationFavorite favorite = new ConversationFavorite();
        favorite.setUserId(userId);
        favorite.setMessageId(messageId);
        favorite.setTitle(title);
        favorite.setCreateTime(LocalDateTime.now());
        favoriteMapper.insert(favorite);
        return favorite;
    }

    public void removeFavorite(Long favoriteId) {
        favoriteMapper.deleteById(favoriteId);
    }

    public List<ConversationFavorite> getUserFavorites(Long userId) {
        return favoriteMapper.selectList(new QueryWrapper<ConversationFavorite>()
                .eq("user_id", userId).orderByDesc("create_time"));
    }

    public ConversationTemplate createTemplate(String title, String content, String category,
                                               Boolean isPublic, Long createUserId) {
        ConversationTemplate template = new ConversationTemplate();
        template.setTitle(title);
        template.setContent(content);
        template.setCategory(category);
        template.setIsPublic(isPublic);
        template.setCreateUserId(createUserId);
        template.setUsageCount(0);
        template.setCreateTime(LocalDateTime.now());
        templateMapper.insert(template);
        return template;
    }

    public List<ConversationTemplate> getPublicTemplates() {
        return templateMapper.selectList(new QueryWrapper<ConversationTemplate>()
                .eq("is_public", true).orderByDesc("usage_count"));
    }

    public List<ConversationTemplate> getUserTemplates(Long userId) {
        return templateMapper.selectList(new QueryWrapper<ConversationTemplate>()
                .eq("create_user_id", userId).orderByDesc("create_time"));
    }

    public void useTemplate(Long templateId) {
        templateMapper.update(null, new UpdateWrapper<ConversationTemplate>().eq("id", templateId)
                .setSql("usage_count = usage_count + 1"));
    }

    public void deleteTemplate(Long templateId) {
        templateMapper.deleteById(templateId);
    }
}
