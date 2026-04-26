package com.afs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.afs.entity.Knowledge;

/**
 * 知识库数据访问层接口
 *
 * 继承 MyBatis-Plus 的 BaseMapper，提供 Knowledge 实体的基础 CRUD 操作。
 *
 * 支持的操作：
 * - selectById: 根据 ID 查询知识条目
 * - insert: 插入新知识条目
 * - updateById: 根据 ID 更新知识条目
 * - deleteById: 根据 ID 删除知识条目
 * - selectList: 查询所有知识或按分类查询
 */
public interface KnowledgeMapper extends BaseMapper<Knowledge> {
}
