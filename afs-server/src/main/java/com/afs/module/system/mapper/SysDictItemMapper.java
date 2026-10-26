package com.afs.module.system.mapper;

import com.afs.module.system.entity.SysDictItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 字典项数据访问接口
 *
 * 基于 MyBatis-Plus 提供 sys_dict_item 表的增删改查。
 *
 * @author guanwx
 */
@Mapper
public interface SysDictItemMapper extends BaseMapper<SysDictItem> {
}
