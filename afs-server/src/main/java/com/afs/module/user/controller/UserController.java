package com.afs.module.user.controller;

import com.afs.security.JwtUtil;
import com.afs.security.TokenBlacklistService;
import com.afs.common.annotation.RequireRole;
import com.afs.common.Result;
import com.afs.module.user.dto.LoginDTO;
import com.afs.module.user.dto.RegisterDTO;
import com.afs.module.user.dto.UserProfileUpdateDTO;
import com.afs.module.user.dto.UserUpsertDTO;
import com.afs.module.user.entity.User;
import com.afs.module.user.service.UserService;
import com.afs.module.user.vo.LoginVO;
import com.afs.module.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理控制器
 *
 * 提供用户注册、登录、登出、个人信息管理以及管理员用户管理接口。
 *
 * @author guanwx
 */
@Tag(name = "用户管理", description = "用户注册、登录、个人信息管理、管理员用户管理等接口")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user")
public class UserController {

    /** 用户服务 */
    private final UserService userService;
    /** JWT 工具类，用于生成与解析令牌 */
    private final JwtUtil jwtUtil;
    /** 令牌黑名单服务，用于登出吊销令牌 */
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * 用户注册
     *
     * @param request 注册请求（用户名、密码）
     * @return 注册成功的用户信息
     */
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterDTO request) {
        User user = userService.register(request.getUsername(), request.getPassword());
        return Result.success("注册成功", UserVO.from(user));
    }

    /**
     * 用户登录
     *
     * @param request 登录请求（用户名、密码）
     * @return 登录令牌与用户信息
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO request) {
        User user = userService.login(request.getUsername(), request.getPassword());
        return Result.success("登录成功", LoginVO.from(jwtUtil.generateToken(user.getId(), user.getRole()), user));
    }

    /**
     * 用户登出（吊销当前令牌）
     *
     * @param request HTTP 请求，从 Authorization 头解析令牌
     * @return 操作结果
     */
    @Operation(summary = "用户登出（吊销当前令牌）")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            // 黑名单 TTL 与令牌剩余有效期一致，令牌自然过期后自动清除
            tokenBlacklistService.revoke(token, jwtUtil.getRemainingSeconds(token));
        }
        return Result.success("已退出登录");
    }

    /**
     * 获取用户详情
     *
     * @param id 用户 ID
     * @return 用户信息
     */
    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    public Result<UserVO> getUser(
            @Parameter(description = "用户 ID", required = true) @PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return Result.notFound("用户不存在");
        }
        return Result.success(UserVO.from(user));
    }

    /**
     * 更新当前用户信息
     *
     * @param request 个人资料更新请求（字段可选）
     * @return 更新后的用户信息
     */
    @Operation(summary = "更新当前用户信息")
    @PutMapping("/update")
    public Result<UserVO> updateUser(@RequestBody UserProfileUpdateDTO request) {
        User user = userService.getUserById(request.getId());
        if (user == null) {
            return Result.notFound("用户不存在");
        }
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        User updatedUser = userService.updateUser(user);
        return Result.success("更新成功", UserVO.from(updatedUser));
    }

    /**
     * 获取所有用户列表（管理员接口）
     *
     * @return 用户列表
     */
    @Operation(summary = "获取所有用户列表（管理员接口）")
    @RequireRole
    @GetMapping("/list")
    public Result<List<UserVO>> getAllUsers() {
        List<UserVO> userList = userService.getAllUsers().stream()
                .map(UserVO::from)
                .collect(Collectors.toList());
        return Result.success(userList);
    }

    /**
     * 创建用户（管理员接口）
     *
     * @param request 用户创建请求
     * @return 创建成功的用户信息
     */
    @Operation(summary = "创建用户（管理员接口）")
    @RequireRole
    @PostMapping("/create")
    public Result<UserVO> createUser(@RequestBody UserUpsertDTO request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setAvatar(request.getAvatar());
        user.setRole(request.getRole());
        User createdUser = userService.createUser(user);
        return Result.success("创建成功", UserVO.from(createdUser));
    }

    /**
     * 管理员更新用户信息
     *
     * @param id 用户 ID
     * @param request 用户更新请求
     * @return 更新后的用户信息
     */
    @Operation(summary = "管理员更新用户信息")
    @RequireRole
    @PutMapping("/admin/update/{id}")
    public Result<UserVO> updateUserByAdmin(
            @Parameter(description = "用户 ID", required = true) @PathVariable Long id,
            @RequestBody UserUpsertDTO request) {
        User req = new User();
        req.setNickname(request.getNickname());
        req.setPhone(request.getPhone());
        req.setEmail(request.getEmail());
        req.setAvatar(request.getAvatar());
        req.setRole(request.getRole());
        req.setPassword(request.getPassword());
        User updatedUser = userService.updateUserByAdmin(id, req);
        return Result.success("更新成功", UserVO.from(updatedUser));
    }

    /**
     * 删除用户（管理员接口）
     *
     * @param id 用户 ID
     * @return 操作结果
     */
    @Operation(summary = "删除用户（管理员接口）")
    @RequireRole
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteUser(
            @Parameter(description = "用户 ID", required = true) @PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success("删除成功");
    }
}
