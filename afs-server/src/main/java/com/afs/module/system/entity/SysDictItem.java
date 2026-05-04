package com.afs.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典项实体
 */
@Data
@TableName("sys_dict_item")
public class SysDictItem {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 所属字典类型编码
     */
    private String dictCode;
    
    /**
     * 字典项编码
     */
    private String itemCode;
    
    /**
     * 字典项名称
     */
    private String itemName;
    
    /**
     * 字典项值
     */
    private String itemValue;
    
    /**
     * 排序
     */
    private Integer sort;
    
    /**
     * 状态：0-停用 1-启用
     */
    private Integer status;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
