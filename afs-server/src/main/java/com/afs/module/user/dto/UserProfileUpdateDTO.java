package com.afs.module.user.dto;

import lombok.Data;

/**
 * 用户个人资料更新请求（字段可选，null 表示不修改）
 *
 * @author guanwx
 */
@Data
public class UserProfileUpdateDTO {

    /** 用户 ID */
    private Long id;

    /** 昵称 */
    private String nickname;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 头像路径 */
    private String avatar;
}
