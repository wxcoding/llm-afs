package com.afs.module.scamcase.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("scam_case")
public class ScamCase {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String type;

    private String content;

    private String tips;

    private LocalDateTime createTime;
}
