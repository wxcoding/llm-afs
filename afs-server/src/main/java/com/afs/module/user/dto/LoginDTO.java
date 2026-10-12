package com.afs.module.user.dto;

import lombok.Data;

/**
 * 用户登录请求
 */
@Data
public class LoginDTO {

    private String username;

    private String password;
}
