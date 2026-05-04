package com.afs.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("knowledge")
public class Knowledge {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String category;

    private String content;

    private String documentType;

    private String sourceFile;

    private Long fileSize;

    private Integer charCount;

    private LocalDateTime updateTime;

    private LocalDateTime createTime;
}
