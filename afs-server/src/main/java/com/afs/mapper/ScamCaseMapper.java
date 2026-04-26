package com.afs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.afs.entity.ScamCase;

/**
 * 诈骗案例数据访问层接口
 *
 * 继承 MyBatis-Plus 的 BaseMapper，提供 ScamCase 实体的基础 CRUD 操作。
 *
 * 支持的操作：
 * - selectById: 根据 ID 查询案例
 * - insert: 插入新案例
 * - updateById: 根据 ID 更新案例
 * - deleteById: 根据 ID 删除案例
 * - selectList: 查询所有案例或按类型查询
 */
public interface ScamCaseMapper extends BaseMapper<ScamCase> {
}
