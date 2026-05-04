package com.afs.module.search.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("hot_search")
public class HotSearch {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String keyword;
    private Integer searchCount;
    private String period;
    private LocalDateTime createTime;
}
