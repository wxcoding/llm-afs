package com.afs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 诈骗案例实体类
 *
 * 对应数据库中的 scam_case 表
 *
 * 存储各类诈骗案例的详细信息，包括案例描述和防范提示，用于 RAG 知识库检索。
 *
 * 字段说明：
 * - id: 案例唯一标识，自增主键
 * - title: 案例标题，如"冒充公检法诈骗"
 * - type: 案例类型，如"电信诈骗"、"网络诈骗"、"投资诈骗"等
 * - content: 案例详细经过描述
 * - tips: 防范提示和识别方法
 * - createTime: 案例添加时间
 */
@Data
@TableName("scam_case")
public class ScamCase {

    /** 案例唯一标识，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 案例标题，如"冒充公检法诈骗" */
    private String title;

    /** 案例类型，如"电信诈骗"、"网络诈骗"、"情感诈骗"、"投资诈骗" */
    private String type;

    /** 案例详细经过描述 */
    private String content;

    /** 防范提示和识别方法 */
    private String tips;

    /** 案例添加时间 */
    private LocalDateTime createTime;
}
