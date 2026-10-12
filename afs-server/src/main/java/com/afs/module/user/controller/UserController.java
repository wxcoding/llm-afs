package com.afs.module.user.controller;

import com.afs.common.Result;
import com.afs.module.user.dto.LoginDTO;
import com.afs.module.user.dto.RegisterDTO;
import com.afs.module.user.dto.UserProfileUpdateDTO;
import com.afs.module.user.dto.UserUpsertDTO;
import com.afs.module.user.entity.User;
import com.afs.module.user.service.UserService;
import com.afs.module.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "用户管理", description = "用户注册、登录、个人信息管理、管理员用户管理等接口")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user")
@CrossOrigin
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<UserVO> register(@RequestBody RegisterDTO request) {
        User user = userService.register(request.getUsername(), request.getPassword());
        return Result.success("注册成功", UserVO.from(user));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<UserVO> login(@RequestBody LoginDTO request) {
        User user = userService.login(request.getUsername(), request.getPassword());
        return Result.success("登录成功", UserVO.from(user));
    }

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

    @Operation(summary = "获取所有用户列表（管理员接口）")
    @GetMapping("/list")
    public Result<List<UserVO>> getAllUsers() {
        List<UserVO> userList = userService.getAllUsers().stream()
                .map(UserVO::from)
                .collect(Collectors.toList());
        return Result.success(userList);
    }

    @Operation(summary = "创建用户（管理员接口）")
    @PostMapping("/create")
    public Result<UserVO> createUser(@RequestBody UserUpsertDTO request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setAvatar(request.getAvatar());
        User createdUser = userService.createUser(user);
        return Result.success("创建成功", UserVO.from(createdUser));
    }

    @Operation(summary = "管理员更新用户信息")
    @PutMapping("/admin/update/{id}")
    public Result<UserVO> updateUserByAdmin(
            @Parameter(description = "用户 ID", required = true) @PathVariable Long id,
            @RequestBody UserUpsertDTO request) {
        User req = new User();
        req.setNickname(request.getNickname());
        req.setPhone(request.getPhone());
        req.setEmail(request.getEmail());
        req.setAvatar(request.getAvatar());
        req.setPassword(request.getPassword());
        User updatedUser = userService.updateUserByAdmin(id, req);
        return Result.success("更新成功", UserVO.from(updatedUser));
    }

    @Operation(summary = "删除用户（管理员接口）")
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteUser(
            @Parameter(description = "用户 ID", required = true) @PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success("删除成功");
    }
}
