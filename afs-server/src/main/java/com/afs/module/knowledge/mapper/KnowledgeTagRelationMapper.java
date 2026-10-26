package com.afs.module.knowledge.mapper;

import com.afs.module.knowledge.entity.KnowledgeTagRelation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库与标签关联关系 Mapper，负责 knowledge_tag_relation 表的数据访问
 *
 * @author guanwx
 */
@Mapper
public interface KnowledgeTagRelationMapper extends BaseMapper<KnowledgeTagRelation> {
}