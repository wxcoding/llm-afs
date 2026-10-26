package com.afs.module.sysconfig.mapper;

import com.afs.module.sysconfig.entity.SystemConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统配置数据访问接口
 *
 * 基于 MyBatis-Plus 提供 system_config 表的增删改查能力
 *
 * @author guanwx
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {
}
