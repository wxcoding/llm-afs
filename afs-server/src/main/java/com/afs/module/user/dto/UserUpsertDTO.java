package com.afs.module.user.dto;

import lombok.Data;

/**
 * 管理员创建/更新用户请求
 */
@Data
public class UserUpsertDTO {

    private String username;

    private String password;

    private String nickname;

    private String phone;

    private String email;

    private String avatar;

    /** 角色：user / admin；为空时默认为 user */
    private String role;
}
