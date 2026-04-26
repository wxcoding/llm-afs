package com.afs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * 对应数据库中的 user 表（双引号包裹因 "user" 是 PostgreSQL 保留关键字）
 *
 * 字段说明：
 * - id: 用户唯一标识，自增主键
 * - username: 用户名，用于登录，唯一约束
 * - password: 密码（已加密存储）
 * - nickname: 昵称，用于显示
 * - avatar: 头像 URL
 * - phone: 手机号
 * - email: 电子邮箱
 * - createTime: 注册时间
 * - updateTime: 最后更新时间
 */
@Data
@TableName("\"user\"")
public class User {

    /** 用户唯一标识，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名，用于登录，唯一约束 */
    private String username;

    /** 密码（建议使用 BCrypt 等加密算法存储） */
    private String password;

    /** 昵称，用于界面显示 */
    private String nickname;

    /** 头像 URL 地址 */
    private String avatar;

    /** 手机号 */
    private String phone;

    /** 电子邮箱 */
    private String email;

    /** 注册时间，自动设置为当前时间 */
    private LocalDateTime createTime;

    /** 最后更新时间，自动更新 */
    private LocalDateTime updateTime;
}
