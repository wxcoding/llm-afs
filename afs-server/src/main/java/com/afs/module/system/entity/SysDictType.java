package com.afs.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典类型实体
 */
@Data
@TableName("sys_dict_type")
public class SysDictType {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 字典类型编码
     */
    private String dictCode;
    
    /**
     * 字典类型名称
     */
    private String dictName;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 状态：0-停用 1-启用
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
