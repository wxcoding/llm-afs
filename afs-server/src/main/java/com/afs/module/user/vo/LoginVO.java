package com.afs.module.user.vo;

import com.afs.module.user.entity.User;
import lombok.Builder;
import lombok.Data;

/**
 * 登录结果 VO
 *
 * 携带访问令牌与用户信息。
 */
@Data
@Builder
public class LoginVO {

    private String token;

    private UserVO user;

    public static LoginVO from(String token, User user) {
        return LoginVO.builder()
                .token(token)
                .user(UserVO.from(user))
                .build();
    }
}
