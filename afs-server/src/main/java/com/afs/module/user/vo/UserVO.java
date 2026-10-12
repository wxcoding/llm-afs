package com.afs.module.user.vo;

import com.afs.module.user.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息视图对象（不含密码等敏感字段）
 */
@Data
public class UserVO {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String phone;

    private String email;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public static UserVO from(User user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname() != null ? user.getNickname() : "");
        vo.setAvatar(user.getAvatar() != null ? user.getAvatar() : "");
        vo.setPhone(user.getPhone() != null ? user.getPhone() : "");
        vo.setEmail(user.getEmail() != null ? user.getEmail() : "");
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());
        return vo;
    }
}
