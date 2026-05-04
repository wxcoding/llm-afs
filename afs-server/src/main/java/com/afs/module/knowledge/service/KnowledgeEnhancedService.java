package com.afs.module.knowledge.service;

import com.afs.enums.KnowledgeStatus;
import com.afs.module.knowledge.entity.Knowledge;
import com.afs.module.knowledge.entity.KnowledgeTag;
import com.afs.module.knowledge.entity.KnowledgeTagRelation;
import com.afs.module.knowledge.entity.KnowledgeVersion;
import com.afs.module.knowledge.entity.KnowledgeAudit;
import com.afs.module.knowledge.mapper.KnowledgeMapper;
import com.afs.module.knowledge.mapper.KnowledgeTagMapper;
import com.afs.module.knowledge.mapper.KnowledgeTagRelationMapper;
import com.afs.module.knowledge.mapper.KnowledgeVersionMapper;
import com.afs.module.knowledge.mapper.KnowledgeAuditMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库增强服务类
 * 
 * 负责知识库标签管理、版本控制、审核流程等增强功能
 */
@Service
public class KnowledgeEnhancedService {

    @Autowired
    private KnowledgeTagMapper tagMapper;
    @Autowired
    private KnowledgeTagRelationMapper tagRelationMapper;
    @Autowired
    private KnowledgeVersionMapper versionMapper;
    @Autowired
    private KnowledgeAuditMapper auditMapper;
    @Autowired
    private KnowledgeMapper knowledgeMapper;
    @Autowired
    private RagService ragService;

    public KnowledgeTag createTag(String name, String color, String description) {
        KnowledgeTag tag = new KnowledgeTag();
        tag.setName(name);
        tag.setColor(color);
        tag.setDescription(description);
        tag.setKnowledgeCount(0);
        tag.setCreateTime(LocalDateTime.now());
        tagMapper.insert(tag);
        return tag;
    }

    public List<KnowledgeTag> getAllTags() {
        return tagMapper.selectList(new QueryWrapper<>());
    }

    public void deleteTag(Long tagId) {
        tagRelationMapper.delete(new QueryWrapper<KnowledgeTagRelation>().eq("tag_id", tagId));
        tagMapper.deleteById(tagId);
    }

    public void addTagToKnowledge(Long knowledgeId, Long tagId) {
        KnowledgeTagRelation relation = new KnowledgeTagRelation();
        relation.setKnowledgeId(knowledgeId);
        relation.setTagId(tagId);
        relation.setCreateTime(LocalDateTime.now());
        tagRelationMapper.insert(relation);

        tagMapper.update(null, new UpdateWrapper<KnowledgeTag>().eq("id", tagId)
                .setSql("knowledge_count = knowledge_count + 1"));
    }

    public void removeTagFromKnowledge(Long knowledgeId, Long tagId) {
        tagRelationMapper.delete(new QueryWrapper<KnowledgeTagRelation>()
                .eq("knowledge_id", knowledgeId).eq("tag_id", tagId));
        tagMapper.update(null, new UpdateWrapper<KnowledgeTag>().eq("id", tagId)
                .setSql("knowledge_count = knowledge_count - 1"));
    }

    public List<KnowledgeTag> getTagsForKnowledge(Long knowledgeId) {
        List<KnowledgeTagRelation> relations = tagRelationMapper.selectList(
                new QueryWrapper<KnowledgeTagRelation>().eq("knowledge_id", knowledgeId));
        List<Long> tagIds = relations.stream().map(KnowledgeTagRelation::getTagId).collect(Collectors.toList());
        if (tagIds.isEmpty()) return List.of();
        return tagMapper.selectList(new QueryWrapper<KnowledgeTag>().in("id", tagIds));
    }

    public KnowledgeVersion saveVersion(Long knowledgeId, String title, String content, String changeSummary, Long operatorId) {
        KnowledgeVersion version = new KnowledgeVersion();
        version.setKnowledgeId(knowledgeId);
        version.setTitle(title);
        version.setContent(content);
        version.setChangeSummary(changeSummary);
        version.setOperatorId(operatorId);
        version.setCreateTime(LocalDateTime.now());
        versionMapper.insert(version);
        return version;
    }

    public List<KnowledgeVersion> getVersionHistory(Long knowledgeId) {
        return versionMapper.selectList(new QueryWrapper<KnowledgeVersion>()
                .eq("knowledge_id", knowledgeId).orderByDesc("create_time"));
    }

    public KnowledgeVersion rollbackToVersion(Long versionId) {
        KnowledgeVersion version = versionMapper.selectById(versionId);
        if (version != null) {
            Knowledge knowledge = knowledgeMapper.selectById(version.getKnowledgeId());
            saveVersion(knowledge.getId(), knowledge.getTitle(), knowledge.getContent(), "回滚到版本 " + versionId, null);
            knowledge.setTitle(version.getTitle());
            knowledge.setContent(version.getContent());
            knowledgeMapper.updateById(knowledge);
        }
        return version;
    }

    public KnowledgeAudit submitForAudit(Long knowledgeId, String title, String content, String category, Long submitUserId) {
        // 处理知识记录
        Knowledge knowledge;
        if (knowledgeId != null) {
            // 更新已有知识
            knowledge = knowledgeMapper.selectById(knowledgeId);
            if (knowledge != null) {
                knowledge.setStatus(KnowledgeStatus.PENDING_REVIEW.getCode());
                knowledgeMapper.updateById(knowledge);
            }
        } else {
            // 新增知识（待审核状态）
            knowledge = new Knowledge();
            knowledge.setTitle(title);
            knowledge.setContent(content);
            knowledge.setCategory(category);
            knowledge.setStatus(KnowledgeStatus.PENDING_REVIEW.getCode());
            knowledge.setCreateTime(LocalDateTime.now());
            knowledgeMapper.insert(knowledge);
            knowledgeId = knowledge.getId();
        }

        KnowledgeAudit audit = new KnowledgeAudit();
        audit.setKnowledgeId(knowledgeId);
        audit.setTitle(title);
        audit.setContent(content);
        audit.setCategory(category);
        audit.setAuditStatus("pending");
        audit.setSubmitUserId(submitUserId);
        audit.setSubmitTime(LocalDateTime.now());
        auditMapper.insert(audit);
        return audit;
    }

    public List<KnowledgeAudit> getPendingAudits() {
        return auditMapper.selectList(new QueryWrapper<KnowledgeAudit>()
                .eq("audit_status", "pending").orderByDesc("submit_time"));
    }

    public KnowledgeAudit approveAudit(Long auditId, Long auditUserId, String comment) {
        KnowledgeAudit audit = auditMapper.selectById(auditId);
        if (audit != null) {
            audit.setAuditStatus("approved");
            audit.setAuditUserId(auditUserId);
            audit.setAuditComment(comment);
            audit.setAuditTime(LocalDateTime.now());
            auditMapper.updateById(audit);

            if (audit.getKnowledgeId() != null) {
                // 更新现有知识
                Knowledge knowledge = new Knowledge();
                knowledge.setId(audit.getKnowledgeId());
                knowledge.setTitle(audit.getTitle());
                knowledge.setContent(audit.getContent());
                knowledge.setCategory(audit.getCategory());
                knowledge.setStatus(KnowledgeStatus.ACTIVE.getCode());
                knowledgeMapper.updateById(knowledge);

                // 同步向量库（失败不影响审核）
                try {
                    ragService.deleteKnowledgeDocument(audit.getKnowledgeId());
                    if (audit.getContent() != null && !audit.getContent().isEmpty()) {
                        ragService.addKnowledgeDocument(
                                audit.getKnowledgeId(),
                                audit.getTitle(),
                                audit.getCategory(),
                                audit.getContent()
                        );
                    }
                } catch (Exception e) {
                    System.err.println("同步向量库失败: " + e.getMessage());
                }
            } else {
                // 新增知识
                Knowledge knowledge = new Knowledge();
                knowledge.setTitle(audit.getTitle());
                knowledge.setContent(audit.getContent());
                knowledge.setCategory(audit.getCategory());
                knowledge.setStatus(KnowledgeStatus.ACTIVE.getCode());
                knowledge.setCreateTime(LocalDateTime.now());
                knowledge.setUpdateTime(LocalDateTime.now());
                knowledgeMapper.insert(knowledge);
                // 更新审核记录的知识ID
                audit.setKnowledgeId(knowledge.getId());
                auditMapper.updateById(audit);

                // 同步向量库（失败不影响审核）
                try {
                    if (knowledge.getContent() != null && !knowledge.getContent().isEmpty()) {
                        ragService.addKnowledgeDocument(
                                knowledge.getId(),
                                knowledge.getTitle(),
                                knowledge.getCategory(),
                                knowledge.getContent()
                        );
                    }
                } catch (Exception e) {
                    System.err.println("同步向量库失败: " + e.getMessage());
                }
            }
        }
        return audit;
    }

    public KnowledgeAudit rejectAudit(Long auditId, Long auditUserId, String comment) {
        KnowledgeAudit audit = auditMapper.selectById(auditId);
        if (audit != null) {
            audit.setAuditStatus("rejected");
            audit.setAuditUserId(auditUserId);
            audit.setAuditComment(comment);
            audit.setAuditTime(LocalDateTime.now());
            auditMapper.updateById(audit);

            // 更新知识状态为已拒绝
            if (audit.getKnowledgeId() != null) {
                Knowledge knowledge = knowledgeMapper.selectById(audit.getKnowledgeId());
                if (knowledge != null) {
                    knowledge.setStatus(KnowledgeStatus.REJECTED.getCode());
                    knowledgeMapper.updateById(knowledge);
                }
            }
        }
        return audit;
    }
}
