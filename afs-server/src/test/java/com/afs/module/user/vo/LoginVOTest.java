package com.afs.module.user.vo;

import com.afs.module.user.entity.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LoginVO 单元测试
 *
 * 覆盖 LoginVO.from 的组装逻辑：携带令牌与转换后的用户信息。
 */
class LoginVOTest {

    /** from：令牌与用户信息被正确组装进登录结果 */
    @Test
    void from_buildsTokenAndUserVO() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");

        LoginVO vo = LoginVO.from("token-abc", user);

        assertThat(vo.getToken()).isEqualTo("token-abc");
        assertThat(vo.getUser()).isNotNull();
        assertThat(vo.getUser().getId()).isEqualTo(1L);
        assertThat(vo.getUser().getUsername()).isEqualTo("alice");
    }
}
