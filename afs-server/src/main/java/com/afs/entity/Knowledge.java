package com.afs.entity;

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
    private LocalDateTime createTime;
}
