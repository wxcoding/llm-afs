package com.afs.module.knowledge.dto;

import lombok.Data;

/**
 * 创建知识标签请求
 */
@Data
public class CreateTagDTO {

    private String name;

    private String color;

    private String description;
}
