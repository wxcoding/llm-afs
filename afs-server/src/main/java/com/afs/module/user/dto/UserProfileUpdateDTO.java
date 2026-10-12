package com.afs.module.user.dto;

import lombok.Data;

/**
 * 用户个人资料更新请求（字段可选，null 表示不修改）
 */
@Data
public class UserProfileUpdateDTO {

    private Long id;

    private String nickname;

    private String phone;

    private String email;

    private String avatar;
}
