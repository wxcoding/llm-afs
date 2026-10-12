package com.afs.module.user.dto;

import lombok.Data;

/**
 * 用户注册请求
 */
@Data
public class RegisterDTO {

    private String username;

    private String password;
}
