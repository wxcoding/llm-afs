package com.afs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.afs.entity.User;

/**
 * 用户数据访问层接口
 *
 * 继承 MyBatis-Plus 的 BaseMapper，提供 User 实体的基础 CRUD 操作。
 * 无需编写 XML 文件或注解 SQL，所有基本操作自动生成。
 *
 * 支持的操作：
 * - selectById: 根据 ID 查询用户
 * - insert: 插入用户
 * - updateById: 根据 ID 更新用户
 * - deleteById: 根据 ID 删除用户
 * - selectList: 查询所有用户
 */
public interface UserMapper extends BaseMapper<User> {
}
