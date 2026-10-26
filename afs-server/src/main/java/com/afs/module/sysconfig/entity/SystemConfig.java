package com.afs.module.sysconfig.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 系统配置实体，对应 system_config 表
 *
 * 存储系统参数配置项（如模型参数、系统提示词等）
 *
 * @author guanwx
 */
@Data
@TableName("system_config")
public class SystemConfig {
    /** 配置项 ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 配置键 */
    private String configKey;
    /** 配置值 */
    private String configValue;
    /** 配置类型（如 system） */
    private String configType;
    /** 配置项描述 */
    private String description;
    /** 是否允许编辑 */
    private Boolean isEditable;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
