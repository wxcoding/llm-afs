package com.afs.module.knowledge.mapper;

import com.afs.module.knowledge.entity.KnowledgeAudit;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库审核记录 Mapper，负责 knowledge_audit 表的数据访问
 *
 * @author guanwx
 */
@Mapper
public interface KnowledgeAuditMapper extends BaseMapper<KnowledgeAudit> {
}