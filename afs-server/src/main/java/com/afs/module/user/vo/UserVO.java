package com.afs.module.user.vo;

import com.afs.module.user.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息视图对象（不含密码等敏感字段）
 *
 * @author guanwx
 */
@Data
public class UserVO {

    /** 用户 ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 头像路径 */
    private String avatar;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 用户角色：user / admin */
    private String role;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /**
     * 从用户实体构建视图对象
     *
     * @param user 用户实体
     * @return 用户视图对象，user 为 null 时返回 null
     */
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
        vo.setRole(user.getRole() != null ? user.getRole() : "user");
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());
        return vo;
    }
}
