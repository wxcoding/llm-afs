package com.afs.module.user.controller;

import com.afs.security.JwtUtil;
import com.afs.security.TokenBlacklistService;
import com.afs.exception.BusinessException;
import com.afs.exception.GlobalExceptionHandler;
import com.afs.module.user.entity.User;
import com.afs.module.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserController 单元测试
 *
 * 使用 MockMvc standaloneSetup 加载控制器与全局异常处理器，
 * 覆盖注册、登录、查询、个人资料更新、管理员增删改查各端点的正常路径，
 * 以及用户名重复、登录失败、用户不存在等业务异常路径。
 * 说明：当前类路径无 Bean Validation 实现（hibernate-validator），
 * @Valid 注解校验实际不生效，故参数校验场景以业务异常路径代替。
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Spring 6 的 JSON 转换器默认不声明 charset，而 MockHttpServletResponse 按
        // ISO-8859-1 解码会导致中文断言乱码，这里显式声明 UTF-8
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setDefaultCharset(StandardCharsets.UTF_8);
        // 注册全局异常处理器，使业务异常映射为对应的 HTTP 状态码
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserController(userService, jwtUtil, tokenBlacklistService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(jsonConverter)
                .build();
    }

    /** 构造一个带基础字段的测试用户 */
    private User sampleUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setNickname("爱丽丝");
        user.setAvatar("/avatars/default.jpg");
        user.setRole("user");
        user.setCreateTime(LocalDateTime.of(2024, 1, 1, 12, 0));
        return user;
    }

    /** 注册成功：返回 200 与用户信息 */
    @Test
    void register_returnsSuccessWithUserVO() throws Exception {
        when(userService.register("alice", "secret6")).thenReturn(sampleUser());

        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "alice", "password", "secret6"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("注册成功"))
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.nickname").value("爱丽丝"))
                // 响应中不应泄露密码字段
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    /** 注册失败：用户名已存在时由全局异常处理器返回 400 */
    @Test
    void register_returns400WhenUsernameDuplicated() throws Exception {
        when(userService.register(any(), any()))
                .thenThrow(BusinessException.badRequest("用户名已存在"));

        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "alice", "password", "secret6"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名已存在"));
    }

    /** 登录成功：返回 token 与用户信息，token 按用户 ID 签发 */
    @Test
    void login_returnsTokenAndUser() throws Exception {
        when(userService.login("alice", "secret6")).thenReturn(sampleUser());
        when(jwtUtil.generateToken(1L, "user")).thenReturn("token-abc");

        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "alice", "password", "secret6"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.data.token").value("token-abc"))
                .andExpect(jsonPath("$.data.user.id").value(1))
                .andExpect(jsonPath("$.data.user.username").value("alice"));
        verify(jwtUtil).generateToken(1L, "user");
    }

    /** 登录失败：用户名或密码错误时由全局异常处理器返回 401 */
    @Test
    void login_returns401WhenCredentialsInvalid() throws Exception {
        when(userService.login(any(), any()))
                .thenThrow(BusinessException.unauthorized("用户名或密码错误"));

        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "alice", "password", "bad-pass"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    /** 登出成功：吊销当前 Bearer 令牌，黑名单 TTL 取令牌剩余有效期 */
    @Test
    void logout_revokesBearerToken() throws Exception {
        when(jwtUtil.getRemainingSeconds("token-abc")).thenReturn(3600L);

        mockMvc.perform(post("/api/user/logout")
                        .header("Authorization", "Bearer token-abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("已退出登录"));

        verify(tokenBlacklistService).revoke("token-abc", 3600L);
    }

    /** 登出成功：未携带令牌时不触发吊销，仍返回成功（幂等） */
    @Test
    void logout_withoutTokenStillSucceeds() throws Exception {
        mockMvc.perform(post("/api/user/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(tokenBlacklistService, org.mockito.Mockito.never()).revoke(any(), org.mockito.Mockito.anyLong());
    }

    /** 查询用户详情：用户存在时返回 200 与用户信息 */
    @Test
    void getUser_returnsUserVOWhenFound() throws Exception {
        when(userService.getUserById(1L)).thenReturn(sampleUser());

        mockMvc.perform(get("/api/user/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("alice"));
    }

    /** 查询用户详情：用户不存在时返回业务 404 结果（HTTP 200，code=404） */
    @Test
    void getUser_returnsNotFoundResultWhenMissing() throws Exception {
        when(userService.getUserById(99L)).thenReturn(null);

        mockMvc.perform(get("/api/user/{id}", 99L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("用户不存在"));
    }

    /** 更新当前用户：仅非空字段被覆盖后提交更新 */
    @Test
    void updateUser_appliesNonNullFieldsAndReturnsUpdated() throws Exception {
        User existing = sampleUser();
        existing.setPhone("13800000000");
        when(userService.getUserById(1L)).thenReturn(existing);
        // updateUser 返回入参实体，便于断言控制器写入的字段
        when(userService.updateUser(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/user/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "id", 1, "nickname", "新昵称"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("更新成功"))
                .andExpect(jsonPath("$.data.nickname").value("新昵称"));

        // 验证控制器只覆盖了 nickname，未提供的 phone 保持不变
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).updateUser(captor.capture());
        assertThat(captor.getValue().getNickname()).isEqualTo("新昵称");
        assertThat(captor.getValue().getPhone()).isEqualTo("13800000000");
    }

    /** 更新当前用户：用户不存在时返回业务 404 结果 */
    @Test
    void updateUser_returnsNotFoundResultWhenUserMissing() throws Exception {
        when(userService.getUserById(99L)).thenReturn(null);

        mockMvc.perform(put("/api/user/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "id", 99, "nickname", "x"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("用户不存在"));
    }

    /** 管理员查询用户列表：返回全部用户的 VO 列表 */
    @Test
    void getAllUsers_returnsMappedVOList() throws Exception {
        User other = sampleUser();
        other.setId(2L);
        other.setUsername("bob");
        when(userService.getAllUsers()).thenReturn(List.of(sampleUser(), other));

        mockMvc.perform(get("/api/user/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].username").value("alice"))
                .andExpect(jsonPath("$.data[1].username").value("bob"))
                .andExpect(jsonPath("$.data[0].password").doesNotExist());
    }

    /** 管理员创建用户：请求字段完整映射给服务层并返回创建结果 */
    @Test
    void createUser_mapsRequestAndReturnsCreatedUser() throws Exception {
        when(userService.createUser(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        mockMvc.perform(post("/api/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "dave",
                                "password", "secret6",
                                "nickname", "戴夫",
                                "phone", "13900000000",
                                "email", "d@a.com",
                                "avatar", "/avatars/d.png"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("创建成功"))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.username").value("dave"));

        // 验证 DTO 字段被完整映射到 User 实体
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).createUser(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("secret6");
        assertThat(captor.getValue().getNickname()).isEqualTo("戴夫");
        assertThat(captor.getValue().getEmail()).isEqualTo("d@a.com");
    }

    /** 管理员更新用户：路径 ID 与请求字段传递给服务层 */
    @Test
    void updateUserByAdmin_passesIdAndFieldsToService() throws Exception {
        User updated = sampleUser();
        updated.setId(2L);
        updated.setNickname("管理员改名");
        when(userService.updateUserByAdmin(eq(2L), any(User.class))).thenReturn(updated);

        mockMvc.perform(put("/api/user/admin/update/{id}", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nickname", "管理员改名", "email", "m@a.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("更新成功"))
                .andExpect(jsonPath("$.data.nickname").value("管理员改名"));

        // 验证路径变量与请求体字段均被正确传递
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).updateUserByAdmin(eq(2L), captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("m@a.com");
    }

    /** 管理员删除用户：委托服务层删除并返回成功消息 */
    @Test
    void deleteUser_returnsSuccessMessage() throws Exception {
        mockMvc.perform(delete("/api/user/delete/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("删除成功"));
        verify(userService).deleteUser(1L);
    }
}
