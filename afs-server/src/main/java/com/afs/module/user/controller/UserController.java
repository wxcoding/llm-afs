package com.afs.module.user.controller;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public Map<String, Object> register(@RequestBody RegisterDTO request) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = userService.register(request.getUsername(), request.getPassword());
            result.put("success", true);
            result.put("message", "注册成功");
            result.put("user", UserVO.from(user));
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginDTO request) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = userService.login(request.getUsername(), request.getPassword());
            result.put("success", true);
            result.put("message", "登录成功");
            result.put("user", UserVO.from(user));
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    public Map<String, Object> getUser(
            @Parameter(description = "用户 ID", required = true) @PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = userService.getUserById(id);
            if (user == null) {
                result.put("success", false);
                result.put("message", "用户不存在");
            } else {
                result.put("success", true);
                result.put("user", UserVO.from(user));
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @Operation(summary = "更新当前用户信息")
    @PutMapping("/update")
    public Map<String, Object> updateUser(@RequestBody UserProfileUpdateDTO request) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = userService.getUserById(request.getId());
            if (user == null) {
                result.put("success", false);
                result.put("message", "用户不存在");
            } else {
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
                result.put("success", true);
                result.put("message", "更新成功");
                result.put("user", UserVO.from(updatedUser));
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @Operation(summary = "获取所有用户列表（管理员接口）")
    @GetMapping("/list")
    public Map<String, Object> getAllUsers() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<User> users = userService.getAllUsers();
            List<UserVO> userList = users.stream().map(UserVO::from).collect(Collectors.toList());
            result.put("success", true);
            result.put("users", userList);
            result.put("total", userList.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @Operation(summary = "创建用户（管理员接口）")
    @PostMapping("/create")
    public Map<String, Object> createUser(@RequestBody UserUpsertDTO request) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(request.getPassword());
            user.setNickname(request.getNickname());
            user.setPhone(request.getPhone());
            user.setEmail(request.getEmail());
            user.setAvatar(request.getAvatar());
            User createdUser = userService.createUser(user);
            result.put("success", true);
            result.put("message", "创建成功");
            result.put("user", UserVO.from(createdUser));
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @Operation(summary = "管理员更新用户信息")
    @PutMapping("/admin/update/{id}")
    public Map<String, Object> updateUserByAdmin(
            @Parameter(description = "用户 ID", required = true) @PathVariable Long id,
            @RequestBody UserUpsertDTO request) {
        Map<String, Object> result = new HashMap<>();
        try {
            User req = new User();
            req.setNickname(request.getNickname());
            req.setPhone(request.getPhone());
            req.setEmail(request.getEmail());
            req.setAvatar(request.getAvatar());
            req.setPassword(request.getPassword());
            User updatedUser = userService.updateUserByAdmin(id, req);
            result.put("success", true);
            result.put("message", "更新成功");
            result.put("user", UserVO.from(updatedUser));
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @Operation(summary = "删除用户（管理员接口）")
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteUser(
            @Parameter(description = "用户 ID", required = true) @PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            userService.deleteUser(id);
            result.put("success", true);
            result.put("message", "删除成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
