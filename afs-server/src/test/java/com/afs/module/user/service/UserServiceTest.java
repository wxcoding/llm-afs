package com.afs.module.user.service;

import com.afs.exception.BusinessException;
import com.afs.module.user.entity.User;
import com.afs.module.user.mapper.UserMapper;
import com.afs.security.LoginRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserService 单元测试
 *
 * 覆盖用户注册、登录（含历史明文密码升级）、按 ID 查询、资料更新、
 * 用户列表、管理员创建/更新/删除用户等全部公开方法的正常与异常分支。
 * 密码处理使用真实的 BCryptPasswordEncoder，UserMapper 使用 Mockito 打桩。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String DEFAULT_AVATAR = "/avatars/default.jpg";

    @Mock
    private UserMapper userMapper;

    @Mock
    private LoginRateLimiter loginRateLimiter;

    /** 使用真实 BCrypt 加密器，与生产配置保持一致 */
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userMapper, passwordEncoder, loginRateLimiter);
    }

    /** 注册成功：写入默认昵称/头像，密码以 BCrypt 加密存储 */
    @Test
    void register_success_createsUserWithDefaultsAndEncodedPassword() {
        // 用户名未被占用
        when(userMapper.selectOne(any())).thenReturn(null);

        User created = userService.register("alice", "secret6");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getNickname()).isEqualTo("alice");
        assertThat(saved.getAvatar()).isEqualTo(DEFAULT_AVATAR);
        assertThat(saved.getCreateTime()).isNotNull();
        // BCrypt 哈希以 $2 开头且能与明文匹配
        assertThat(saved.getPassword()).startsWith("$2");
        assertThat(passwordEncoder.matches("secret6", saved.getPassword())).isTrue();
        assertThat(created).isSameAs(saved);
    }

    /** 注册失败：用户名已存在时抛出 400 业务异常且不执行插入 */
    @Test
    void register_throwsBadRequestWhenUsernameExists() {
        // 模拟已存在同名用户
        when(userMapper.selectOne(any())).thenReturn(new User());

        assertThatThrownBy(() -> userService.register("alice", "secret6"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名已存在")
                .satisfies(e -> assertThat(((BusinessException) e).getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
        verify(userMapper, never()).insert(any(User.class));
    }

    /** 登录成功：BCrypt 密码校验通过后直接返回用户 */
    @Test
    void login_successWithBcryptPassword() {
        User user = new User();
        user.setUsername("bob");
        user.setPassword(passwordEncoder.encode("secret6"));
        when(userMapper.selectOne(any())).thenReturn(user);

        User result = userService.login("bob", "secret6");

        assertThat(result).isSameAs(user);
        // 已是 BCrypt 密码，不触发升级写库
        verify(userMapper, never()).updateById(any(User.class));
    }

    /** 登录失败：用户不存在时抛出 401 业务异常 */
    @Test
    void login_throwsUnauthorizedWhenUserNotFound() {
        when(userMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> userService.login("ghost", "secret6"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名或密码错误")
                .satisfies(e -> assertThat(((BusinessException) e).getStatus())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    /** 登录失败：BCrypt 密码不匹配时抛出 401 业务异常 */
    @Test
    void login_throwsUnauthorizedWhenBcryptPasswordMismatch() {
        User user = new User();
        user.setUsername("bob");
        user.setPassword(passwordEncoder.encode("secret6"));
        when(userMapper.selectOne(any())).thenReturn(user);

        assertThatThrownBy(() -> userService.login("bob", "wrong-pass"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名或密码错误");
    }

    /** 登录成功：历史明文密码校验通过后自动升级为 BCrypt 并写库 */
    @Test
    void login_upgradesLegacyPlaintextPasswordToBcrypt() {
        User user = new User();
        user.setId(7L);
        user.setUsername("carol");
        user.setPassword("plain123"); // 存量明文密码
        when(userMapper.selectOne(any())).thenReturn(user);

        User result = userService.login("carol", "plain123");

        assertThat(result).isSameAs(user);
        // 密码已升级为 BCrypt 且 updateTime 被刷新
        assertThat(user.getPassword()).startsWith("$2");
        assertThat(passwordEncoder.matches("plain123", user.getPassword())).isTrue();
        assertThat(user.getUpdateTime()).isNotNull();
        verify(userMapper).updateById(user);
    }

    /** 登录失败：历史明文密码不匹配时抛出 401 且不触发升级 */
    @Test
    void login_throwsUnauthorizedWhenPlaintextMismatch() {
        User user = new User();
        user.setUsername("carol");
        user.setPassword("plain123");
        when(userMapper.selectOne(any())).thenReturn(user);

        assertThatThrownBy(() -> userService.login("carol", "other-pass"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名或密码错误");
        verify(userMapper, never()).updateById(any(User.class));
    }

    /** 登录成功：通知限流器清零失败计数，且不记录失败 */
    @Test
    void login_successClearsRateLimitCounter() {
        User user = new User();
        user.setUsername("bob");
        user.setPassword(passwordEncoder.encode("secret6"));
        when(userMapper.selectOne(any())).thenReturn(user);

        userService.login("bob", "secret6");

        verify(loginRateLimiter).checkLocked("bob");
        verify(loginRateLimiter).onSuccess("bob");
        verify(loginRateLimiter, never()).onFailure(any());
    }

    /** 登录失败：通知限流器累计失败次数，且不清零 */
    @Test
    void login_failureIncrementsRateLimitCounter() {
        when(userMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> userService.login("ghost", "secret6"))
                .isInstanceOf(BusinessException.class);

        verify(loginRateLimiter).onFailure("ghost");
        verify(loginRateLimiter, never()).onSuccess(any());
    }

    /** 账号锁定中：限流器抛出 429 业务异常，且不访问数据库 */
    @Test
    void login_propagatesLockedExceptionWithoutQueryingDb() {
        org.mockito.Mockito.doThrow(new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "账号已临时锁定"))
                .when(loginRateLimiter).checkLocked("bob");

        assertThatThrownBy(() -> userService.login("bob", "secret6"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号已临时锁定")
                .satisfies(e -> assertThat(((BusinessException) e).getStatus())
                        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
        verify(userMapper, never()).selectOne(any());
    }

    /** 按 ID 查询：直接委托给 mapper.selectById */
    @Test
    void getUserById_delegatesToMapper() {
        User user = new User();
        user.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(user);

        assertThat(userService.getUserById(1L)).isSameAs(user);
    }

    /** 更新用户：刷新 updateTime、写库后返回最新记录 */
    @Test
    void updateUser_setsUpdateTimeAndReturnsFreshUser() {
        User user = new User();
        user.setId(1L);
        user.setNickname("旧昵称");
        User fresh = new User();
        fresh.setId(1L);
        fresh.setNickname("新昵称");
        // 写库后重新查询得到最新数据
        when(userMapper.selectById(1L)).thenReturn(fresh);

        User result = userService.updateUser(user);

        assertThat(result).isSameAs(fresh);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertThat(captor.getValue().getUpdateTime()).isNotNull();
    }

    /** 查询全部用户：按创建时间倒序委托给 mapper.selectList */
    @Test
    void getAllUsers_returnsListFromMapper() {
        List<User> users = List.of(new User(), new User());
        when(userMapper.selectList(any())).thenReturn(users);

        assertThat(userService.getAllUsers()).isSameAs(users);
    }

    /** 管理员创建用户：补全默认字段并加密密码 */
    @Test
    void createUser_success_appliesDefaultsAndEncodesPassword() {
        when(userMapper.selectOne(any())).thenReturn(null);
        User user = new User();
        user.setUsername("dave");
        user.setPassword("secret6");

        User created = userService.createUser(user);

        assertThat(created.getNickname()).isEqualTo("dave");
        assertThat(created.getAvatar()).isEqualTo(DEFAULT_AVATAR);
        assertThat(created.getCreateTime()).isNotNull();
        assertThat(passwordEncoder.matches("secret6", created.getPassword())).isTrue();
        verify(userMapper).insert(user);
    }

    /** 管理员创建用户：用户名重复时抛出 400 业务异常 */
    @Test
    void createUser_throwsBadRequestWhenUsernameExists() {
        // 模拟已存在同名用户
        when(userMapper.selectOne(any())).thenReturn(new User());
        User user = new User();
        user.setUsername("dave");

        assertThatThrownBy(() -> userService.createUser(user))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名已存在");
        verify(userMapper, never()).insert(any(User.class));
    }

    /** 管理员创建用户：密码为空时保持原样不加密，显式字段不被默认值覆盖 */
    @Test
    void createUser_keepsNullPasswordAndProvidedFieldsUntouched() {
        when(userMapper.selectOne(any())).thenReturn(null);
        User user = new User();
        user.setUsername("erin");
        user.setNickname("小艾");
        user.setAvatar("/avatars/custom.png");
        user.setCreateTime(LocalDateTime.of(2024, 1, 1, 0, 0));

        User created = userService.createUser(user);

        assertThat(created.getPassword()).isNull();
        assertThat(created.getNickname()).isEqualTo("小艾");
        assertThat(created.getAvatar()).isEqualTo("/avatars/custom.png");
        assertThat(created.getCreateTime()).isEqualTo(LocalDateTime.of(2024, 1, 1, 0, 0));
        verify(userMapper).insert(user);
    }

    /** 管理员更新用户：非空字段覆盖旧值，密码加密后写库 */
    @Test
    void updateUserByAdmin_success_updatesFieldsAndEncodesPassword() {
        User existing = new User();
        existing.setId(2L);
        existing.setNickname("旧昵称");
        existing.setPassword(passwordEncoder.encode("old-pass"));
        when(userMapper.selectById(2L)).thenReturn(existing);
        User req = new User();
        req.setNickname("新昵称");
        req.setPhone("13800000000");
        req.setEmail("a@b.com");
        req.setAvatar("/avatars/new.png");
        req.setPassword("new-pass");

        User result = userService.updateUserByAdmin(2L, req);

        assertThat(result).isSameAs(existing);
        assertThat(existing.getNickname()).isEqualTo("新昵称");
        assertThat(existing.getPhone()).isEqualTo("13800000000");
        assertThat(existing.getEmail()).isEqualTo("a@b.com");
        assertThat(existing.getAvatar()).isEqualTo("/avatars/new.png");
        assertThat(passwordEncoder.matches("new-pass", existing.getPassword())).isTrue();
        assertThat(existing.getUpdateTime()).isNotNull();
        verify(userMapper).updateById(existing);
    }

    /** 管理员更新用户：用户不存在时抛出 404 业务异常 */
    @Test
    void updateUserByAdmin_throwsNotFoundWhenUserMissing() {
        when(userMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> userService.updateUserByAdmin(99L, new User()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在")
                .satisfies(e -> assertThat(((BusinessException) e).getStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
        verify(userMapper, never()).updateById(any(User.class));
    }

    /** 管理员更新用户：请求字段为空时保持原值，空密码不触发加密 */
    @Test
    void updateUserByAdmin_ignoresNullAndBlankFields() {
        User existing = new User();
        existing.setId(3L);
        existing.setNickname("保留昵称");
        String oldHash = passwordEncoder.encode("old-pass");
        existing.setPassword(oldHash);
        when(userMapper.selectById(3L)).thenReturn(existing);
        User req = new User(); // 全部字段为 null
        req.setPassword("   "); // 空白密码不应触发更新

        User result = userService.updateUserByAdmin(3L, req);

        assertThat(result.getNickname()).isEqualTo("保留昵称");
        assertThat(result.getPassword()).isEqualTo(oldHash);
        assertThat(result.getUpdateTime()).isNotNull();
        verify(userMapper).updateById(existing);
    }

    /** 删除用户：直接委托给 mapper.deleteById */
    @Test
    void deleteUser_delegatesToMapper() {
        userService.deleteUser(5L);

        verify(userMapper).deleteById(5L);
    }
}
