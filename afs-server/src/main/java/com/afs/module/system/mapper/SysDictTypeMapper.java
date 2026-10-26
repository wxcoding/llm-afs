package com.afs.module.system.mapper;

import com.afs.module.system.entity.SysDictType;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 字典类型数据访问接口
 *
 * 基于 MyBatis-Plus 提供 sys_dict_type 表的增删改查。
 *
 * @author guanwx
 */
@Mapper
public interface SysDictTypeMapper extends BaseMapper<SysDictType> {
}
