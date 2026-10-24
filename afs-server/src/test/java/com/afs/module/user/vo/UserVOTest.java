package com.afs.module.user.vo;

import com.afs.module.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserVO 单元测试
 *
 * 覆盖 UserVO.from 的空指针容错逻辑：user 为 null 返回 null，
 * 可选字段为 null 时降级为空字符串，且不暴露密码字段。
 */
class UserVOTest {

    /** from(null)：直接返回 null，不抛异常 */
    @Test
    void from_nullUser_returnsNull() {
        assertThat(UserVO.from(null)).isNull();
    }

    /** 字段齐全：所有字段完整映射且时间字段保留原值 */
    @Test
    void from_fullUser_mapsAllFields() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setPassword("should-not-leak");
        user.setNickname("爱丽丝");
        user.setAvatar("/avatars/a.png");
        user.setPhone("13800000000");
        user.setEmail("a@b.com");
        LocalDateTime now = LocalDateTime.of(2024, 6, 1, 8, 0);
        user.setCreateTime(now);
        user.setUpdateTime(now);

        UserVO vo = UserVO.from(user);

        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getUsername()).isEqualTo("alice");
        assertThat(vo.getNickname()).isEqualTo("爱丽丝");
        assertThat(vo.getAvatar()).isEqualTo("/avatars/a.png");
        assertThat(vo.getPhone()).isEqualTo("13800000000");
        assertThat(vo.getEmail()).isEqualTo("a@b.com");
        assertThat(vo.getCreateTime()).isEqualTo(now);
        assertThat(vo.getUpdateTime()).isEqualTo(now);
    }

    /** 可选字段为 null：降级为空字符串而不是 null */
    @Test
    void from_userWithNullOptionalFields_defaultsToEmptyStrings() {
        User user = new User();
        user.setId(2L);
        user.setUsername("bob");

        UserVO vo = UserVO.from(user);

        assertThat(vo.getNickname()).isEmpty();
        assertThat(vo.getAvatar()).isEmpty();
        assertThat(vo.getPhone()).isEmpty();
        assertThat(vo.getEmail()).isEmpty();
        assertThat(vo.getCreateTime()).isNull();
        assertThat(vo.getUpdateTime()).isNull();
    }
}
