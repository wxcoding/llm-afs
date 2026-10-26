package com.afs.module.knowledge.mapper;

import com.afs.module.knowledge.entity.KnowledgeVersion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库版本记录 Mapper，负责 knowledge_version 表的数据访问
 *
 * @author guanwx
 */
@Mapper
public interface KnowledgeVersionMapper extends BaseMapper<KnowledgeVersion> {
}