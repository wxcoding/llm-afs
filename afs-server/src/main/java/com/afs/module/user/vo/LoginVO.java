package com.afs.module.user.vo;

import com.afs.module.user.entity.User;
import lombok.Builder;
import lombok.Data;

/**
 * 登录结果 VO
 *
 * 携带访问令牌与用户信息。
 *
 * @author guanwx
 */
@Data
@Builder
public class LoginVO {

    /** 访问令牌（JWT） */
    private String token;

    /** 登录用户信息 */
    private UserVO user;

    /**
     * 构建登录结果
     *
     * @param token 访问令牌
     * @param user 登录用户实体
     * @return 登录结果 VO
     */
    public static LoginVO from(String token, User user) {
        return LoginVO.builder()
                .token(token)
                .user(UserVO.from(user))
                .build();
    }
}
