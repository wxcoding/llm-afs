package com.afs.module.knowledge.dto;

import lombok.Data;

/**
 * 创建知识标签请求
 *
 * @author guanwx
 */
@Data
public class CreateTagDTO {

    /** 标签名称 */
    private String name;

    /** 标签颜色 */
    private String color;

    /** 标签描述 */
    private String description;
}
