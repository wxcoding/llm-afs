package com.afs.module.user.dto;

import lombok.Data;

/**
 * 管理员创建/更新用户请求
 *
 * @author guanwx
 */
@Data
public class UserUpsertDTO {

    /** 用户名（创建时必填） */
    private String username;

    /** 密码（创建时必填，更新时为空表示不修改） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 头像路径 */
    private String avatar;

    /** 角色：user / admin；为空时默认为 user */
    private String role;
}
