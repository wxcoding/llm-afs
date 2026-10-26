package com.afs.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体，对应 user 表
 *
 * 存储系统用户的账号、资料与角色信息。
 *
 * @author guanwx
 */
@Data
@TableName("\"user\"")
public class User {

    /** 用户 ID，主键自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名，登录账号 */
    private String username;

    /** 密码（BCrypt 加密存储） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 头像路径 */
    private String avatar;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 用户角色：user / admin，见 {@link com.afs.common.constant.Roles} */
    private String role;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
