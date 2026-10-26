package com.afs.module.knowledge.mapper;

import com.afs.module.knowledge.entity.KnowledgeTag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库标签 Mapper，负责 knowledge_tag 表的数据访问
 *
 * @author guanwx
 */
@Mapper
public interface KnowledgeTagMapper extends BaseMapper<KnowledgeTag> {
}