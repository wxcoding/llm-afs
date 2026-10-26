package com.afs.module.scamcase.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 诈骗案例实体，对应 scam_case 表
 *
 * 存储诈骗案例的标题、类型、内容及防骗提示
 *
 * @author guanwx
 */
@Data
@TableName("scam_case")
public class ScamCase {

    /** 案例 ID，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 案例标题 */
    private String title;

    /** 案例类型（如电信诈骗、网络诈骗等） */
    private String type;

    /** 案例内容描述 */
    private String content;

    /** 防骗提示 */
    private String tips;

    /** 创建时间 */
    private LocalDateTime createTime;
}
