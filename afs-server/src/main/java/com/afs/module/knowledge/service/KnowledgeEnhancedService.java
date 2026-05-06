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
import com.afs.module.rag.RagService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库增强服务类
 * 
 * 负责知识库的标签管理、版本控制、审核流程等核心增强功能。
 * 该服务作为知识库基础CRUD操作的补充，提供知识库管理所需的高级特性。
 * 
 * <p>主要功能模块：
 * <ul>
 *   <li>标签管理：知识条目标签的增删查改及关联关系维护</li>
 *   <li>版本控制：知识内容的版本记录、历史回溯和回滚操作</li>
 *   <li>审核流程：知识提交审核、管理员审批通过/拒绝、状态同步</li>
 *   <li>向量同步：审核通过后自动同步至向量数据库</li>
 * </ul>
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

    /**
     * 创建知识标签
     * 
     * @param name 标签名称，用于标识和展示
     * @param color 标签颜色，用于前端展示区分
     * @param description 标签描述，说明标签用途
     * @return 创建成功的标签实体，包含自动生成的ID
     */
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

    /**
     * 获取所有标签列表
     * 
     * @return 所有标签实体列表，按创建时间升序排列
     */
    public List<KnowledgeTag> getAllTags() {
        return tagMapper.selectList(new QueryWrapper<>());
    }

    /**
     * 删除标签（级联删除关联关系）
     * 
     * @param tagId 要删除的标签ID
     */
    public void deleteTag(Long tagId) {
        // 先删除关联关系，再删除标签
        tagRelationMapper.delete(new QueryWrapper<KnowledgeTagRelation>().eq("tag_id", tagId));
        tagMapper.deleteById(tagId);
    }

    /**
     * 为知识条目添加标签
     * 
     * @param knowledgeId 知识条目ID
     * @param tagId 标签ID
     */
    public void addTagToKnowledge(Long knowledgeId, Long tagId) {
        // 创建关联记录
        KnowledgeTagRelation relation = new KnowledgeTagRelation();
        relation.setKnowledgeId(knowledgeId);
        relation.setTagId(tagId);
        relation.setCreateTime(LocalDateTime.now());
        tagRelationMapper.insert(relation);

        // 更新标签关联计数
        tagMapper.update(null, new UpdateWrapper<KnowledgeTag>().eq("id", tagId)
                .setSql("knowledge_count = knowledge_count + 1"));
    }

    /**
     * 从知识条目移除标签
     * 
     * @param knowledgeId 知识条目ID
     * @param tagId 标签ID
     */
    public void removeTagFromKnowledge(Long knowledgeId, Long tagId) {
        // 删除关联记录
        tagRelationMapper.delete(new QueryWrapper<KnowledgeTagRelation>()
                .eq("knowledge_id", knowledgeId).eq("tag_id", tagId));
        // 更新标签关联计数
        tagMapper.update(null, new UpdateWrapper<KnowledgeTag>().eq("id", tagId)
                .setSql("knowledge_count = knowledge_count - 1"));
    }

    /**
     * 获取知识条目关联的所有标签
     * 
     * @param knowledgeId 知识条目ID
     * @return 标签列表，如果没有关联则返回空列表
     */
    public List<KnowledgeTag> getTagsForKnowledge(Long knowledgeId) {
        // 查询关联关系
        List<KnowledgeTagRelation> relations = tagRelationMapper.selectList(
                new QueryWrapper<KnowledgeTagRelation>().eq("knowledge_id", knowledgeId));
        // 提取标签ID
        List<Long> tagIds = relations.stream().map(KnowledgeTagRelation::getTagId).collect(Collectors.toList());
        if (tagIds.isEmpty()) return List.of();
        // 查询标签详情
        return tagMapper.selectList(new QueryWrapper<KnowledgeTag>().in("id", tagIds));
    }

    /**
     * 保存知识版本记录
     * 
     * @param knowledgeId 知识条目ID
     * @param title 版本标题
     * @param content 版本内容
     * @param changeSummary 变更摘要，描述本次修改内容
     * @param operatorId 操作人ID
     * @return 创建的版本记录实体
     */
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

    /**
     * 获取知识条目的版本历史
     * 
     * @param knowledgeId 知识条目ID
     * @return 版本历史列表，按创建时间倒序排列（最新版本在前）
     */
    public List<KnowledgeVersion> getVersionHistory(Long knowledgeId) {
        return versionMapper.selectList(new QueryWrapper<KnowledgeVersion>()
                .eq("knowledge_id", knowledgeId).orderByDesc("create_time"));
    }

    /**
     * 回滚到指定版本
     * 
     * <p>回滚逻辑：
     * 1. 先保存当前版本作为历史记录（记录回滚操作）
     * 2. 将知识条目恢复到指定版本的内容
     * 
     * @param versionId 目标版本ID
     * @return 回滚到的版本实体，如果版本不存在则返回null
     */
    public KnowledgeVersion rollbackToVersion(Long versionId) {
        KnowledgeVersion version = versionMapper.selectById(versionId);
        if (version != null) {
            // 获取当前知识状态
            Knowledge knowledge = knowledgeMapper.selectById(version.getKnowledgeId());
            // 保存当前状态作为新版本（记录回滚操作）
            saveVersion(knowledge.getId(), knowledge.getTitle(), knowledge.getContent(), 
                    "回滚到版本 " + versionId, null);
            // 恢复到目标版本内容
            knowledge.setTitle(version.getTitle());
            knowledge.setContent(version.getContent());
            knowledgeMapper.updateById(knowledge);
        }
        return version;
    }

    /**
     * 提交知识审核申请
     * 
     * <p>支持两种场景：
     * 1. 新增知识：knowledgeId为null，创建新的知识条目并提交审核
     * 2. 修改知识：knowledgeId不为null，更新现有知识状态为待审核
     * 
     * @param knowledgeId 知识条目ID（新增时为null）
     * @param title 知识标题
     * @param content 知识内容
     * @param category 知识分类
     * @param submitUserId 提交人ID
     * @return 创建的审核记录实体
     */
    public KnowledgeAudit submitForAudit(Long knowledgeId, String title, String content, String category, Long submitUserId) {
        Knowledge knowledge;
        
        if (knowledgeId != null) {
            // 更新已有知识，设置状态为待审核
            knowledge = knowledgeMapper.selectById(knowledgeId);
            if (knowledge != null) {
                knowledge.setStatus(KnowledgeStatus.PENDING_REVIEW.getCode());
                knowledgeMapper.updateById(knowledge);
            }
        } else {
            // 新增知识，直接创建待审核状态的记录
            knowledge = new Knowledge();
            knowledge.setTitle(title);
            knowledge.setContent(content);
            knowledge.setCategory(category);
            knowledge.setStatus(KnowledgeStatus.PENDING_REVIEW.getCode());
            knowledge.setCreateTime(LocalDateTime.now());
            knowledgeMapper.insert(knowledge);
            knowledgeId = knowledge.getId();
        }

        // 创建审核记录
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

    /**
     * 获取待审核列表
     * 
     * @return 待审核的知识列表，按提交时间倒序排列
     */
    public List<KnowledgeAudit> getPendingAudits() {
        return auditMapper.selectList(new QueryWrapper<KnowledgeAudit>()
                .eq("audit_status", "pending").orderByDesc("submit_time"));
    }

    /**
     * 审核通过
     * 
     * <p>审核通过后的操作：
     * 1. 更新审核记录状态为已通过
     * 2. 更新知识条目状态为已发布（ACTIVE）
     * 3. 同步向量库（失败不影响审核结果）
     * 
     * @param auditId 审核记录ID
     * @param auditUserId 审核人ID
     * @param comment 审核意见
     * @return 更新后的审核记录实体
     */
    public KnowledgeAudit approveAudit(Long auditId, Long auditUserId, String comment) {
        KnowledgeAudit audit = auditMapper.selectById(auditId);
        
        if (audit != null) {
            // 更新审核记录状态
            audit.setAuditStatus("approved");
            audit.setAuditUserId(auditUserId);
            audit.setAuditComment(comment);
            audit.setAuditTime(LocalDateTime.now());
            auditMapper.updateById(audit);

            if (audit.getKnowledgeId() != null) {
                // 更新现有知识状态为已发布
                Knowledge knowledge = new Knowledge();
                knowledge.setId(audit.getKnowledgeId());
                knowledge.setTitle(audit.getTitle());
                knowledge.setContent(audit.getContent());
                knowledge.setCategory(audit.getCategory());
                knowledge.setStatus(KnowledgeStatus.ACTIVE.getCode());
                knowledgeMapper.updateById(knowledge);

                // 同步向量库（失败不影响审核）
                syncToVectorStore(audit.getKnowledgeId(), audit.getTitle(), 
                        audit.getCategory(), audit.getContent());
            } else {
                // 新增知识（审核通过后创建正式记录）
                Knowledge knowledge = new Knowledge();
                knowledge.setTitle(audit.getTitle());
                knowledge.setContent(audit.getContent());
                knowledge.setCategory(audit.getCategory());
                knowledge.setStatus(KnowledgeStatus.ACTIVE.getCode());
                knowledge.setCreateTime(LocalDateTime.now());
                knowledge.setUpdateTime(LocalDateTime.now());
                knowledgeMapper.insert(knowledge);
                
                // 更新审核记录关联的知识ID
                audit.setKnowledgeId(knowledge.getId());
                auditMapper.updateById(audit);

                // 同步向量库
                syncToVectorStore(knowledge.getId(), knowledge.getTitle(), 
                        knowledge.getCategory(), knowledge.getContent());
            }
        }
        return audit;
    }

    /**
     * 审核拒绝
     * 
     * <p>拒绝后的操作：
     * 1. 更新审核记录状态为已拒绝
     * 2. 如果是修改已有知识，更新知识状态为已拒绝
     * 
     * @param auditId 审核记录ID
     * @param auditUserId 审核人ID
     * @param comment 拒绝原因
     * @return 更新后的审核记录实体
     */
    public KnowledgeAudit rejectAudit(Long auditId, Long auditUserId, String comment) {
        KnowledgeAudit audit = auditMapper.selectById(auditId);
        
        if (audit != null) {
            // 更新审核记录状态
            audit.setAuditStatus("rejected");
            audit.setAuditUserId(auditUserId);
            audit.setAuditComment(comment);
            audit.setAuditTime(LocalDateTime.now());
            auditMapper.updateById(audit);

            // 更新知识状态为已拒绝（仅针对已有知识）
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

    /**
     * 同步知识到向量库
     * 
     * <p>先删除旧的向量记录，再添加新的向量记录。
     * 该操作采用静默失败策略，向量同步失败不影响主业务流程。
     * 
     * @param knowledgeId 知识ID
     * @param title 知识标题
     * @param category 知识分类
     * @param content 知识内容
     */
    private void syncToVectorStore(Long knowledgeId, String title, String category, String content) {
        try {
            // 先删除旧的向量记录
            ragService.deleteKnowledgeDocument(knowledgeId);
            
            // 添加新的向量记录（内容不为空时）
            if (content != null && !content.isEmpty()) {
                ragService.addKnowledgeDocument(knowledgeId, title, category, content);
            }
        } catch (Exception e) {
            // 向量同步失败仅记录日志，不影响主流程
            System.err.println("同步向量库失败 [" + knowledgeId + "]: " + e.getMessage());
        }
    }
}
