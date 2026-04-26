package com.afs.controller;

import com.afs.entity.User;
import com.afs.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户控制器
 *
 * 提供用户相关的 REST API 接口，包括注册、登录、个人信息管理等。
 * 所有接口均支持跨域访问（CORS）。
 */
@RestController
@RequestMapping("/api/user")
@CrossOrigin
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户注册
     *
     * @param params 包含 username 和 password 的请求体
     * @return 注册结果，包含成功状态、消息和用户信息
     */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            String username = params.get("username");
            String password = params.get("password");
            User user = userService.register(username, password);
            result.put("success", true);
            result.put("message", "注册成功");
            result.put("user", Map.of("id", user.getId(), "username", user.getUsername(), "nickname", user.getNickname(), "avatar", user.getAvatar() != null ? user.getAvatar() : ""));
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 用户登录
     *
     * @param params 包含 username 和 password 的请求体
     * @return 登录结果，包含成功状态、消息和用户信息
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            String username = params.get("username");
            String password = params.get("password");
            User user = userService.login(username, password);
            result.put("success", true);
            result.put("message", "登录成功");
            result.put("user", Map.of("id", user.getId(), "username", user.getUsername(), "nickname", user.getNickname() != null ? user.getNickname() : "", "avatar", user.getAvatar() != null ? user.getAvatar() : ""));
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取用户详情
     *
     * @param id 用户 ID
     * @return 用户详细信息
     */
    @GetMapping("/{id}")
    public Map<String, Object> getUser(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = userService.getUserById(id);
            if (user == null) {
                result.put("success", false);
                result.put("message", "用户不存在");
            } else {
                result.put("success", true);
                result.put("user", Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "nickname", user.getNickname() != null ? user.getNickname() : "",
                    "avatar", user.getAvatar() != null ? user.getAvatar() : "",
                    "phone", user.getPhone() != null ? user.getPhone() : "",
                    "email", user.getEmail() != null ? user.getEmail() : "",
                    "createTime", user.getCreateTime() != null ? user.getCreateTime().toString() : "",
                    "updateTime", user.getUpdateTime() != null ? user.getUpdateTime().toString() : ""
                ));
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 更新当前用户信息
     *
     * @param params 包含要更新的字段（nickname、phone、email、avatar）
     * @return 更新结果
     */
    @PutMapping("/update")
    public Map<String, Object> updateUser(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long id = Long.parseLong(params.get("id").toString());
            User user = userService.getUserById(id);
            if (user == null) {
                result.put("success", false);
                result.put("message", "用户不存在");
            } else {
                if (params.containsKey("nickname")) {
                    user.setNickname(params.get("nickname").toString());
                }
                if (params.containsKey("phone")) {
                    user.setPhone(params.get("phone").toString());
                }
                if (params.containsKey("email")) {
                    user.setEmail(params.get("email").toString());
                }
                if (params.containsKey("avatar")) {
                    user.setAvatar(params.get("avatar").toString());
                }
                User updatedUser = userService.updateUser(user);
                result.put("success", true);
                result.put("message", "更新成功");
                result.put("user", Map.of(
                    "id", updatedUser.getId(),
                    "username", updatedUser.getUsername(),
                    "nickname", updatedUser.getNickname() != null ? updatedUser.getNickname() : "",
                    "avatar", updatedUser.getAvatar() != null ? updatedUser.getAvatar() : "",
                    "phone", updatedUser.getPhone() != null ? updatedUser.getPhone() : "",
                    "email", updatedUser.getEmail() != null ? updatedUser.getEmail() : "",
                    "createTime", updatedUser.getCreateTime() != null ? updatedUser.getCreateTime().toString() : "",
                    "updateTime", updatedUser.getUpdateTime() != null ? updatedUser.getUpdateTime().toString() : ""
                ));
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取所有用户列表（管理员接口）
     *
     * @return 用户列表
     */
    @GetMapping("/list")
    public Map<String, Object> getAllUsers() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<User> users = userService.getAllUsers();
            List<Map<String, Object>> userList = users.stream().map(user -> Map.<String, Object>of(
                "id", user.getId(),
                "username", user.getUsername(),
                "nickname", user.getNickname() != null ? user.getNickname() : "",
                "avatar", user.getAvatar() != null ? user.getAvatar() : "",
                "phone", user.getPhone() != null ? user.getPhone() : "",
                "email", user.getEmail() != null ? user.getEmail() : "",
                "createTime", user.getCreateTime() != null ? user.getCreateTime().toString() : "",
                "updateTime", user.getUpdateTime() != null ? user.getUpdateTime().toString() : ""
            )).collect(Collectors.toList());
            result.put("success", true);
            result.put("users", userList);
            result.put("total", userList.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 创建用户（管理员接口）
     *
     * @param user 用户对象
     * @return 创建结果
     */
    @PostMapping("/create")
    public Map<String, Object> createUser(@RequestBody User user) {
        Map<String, Object> result = new HashMap<>();
        try {
            User createdUser = userService.createUser(user);
            result.put("success", true);
            result.put("message", "创建成功");
            result.put("user", Map.of(
                "id", createdUser.getId(),
                "username", createdUser.getUsername(),
                "nickname", createdUser.getNickname() != null ? createdUser.getNickname() : "",
                "avatar", createdUser.getAvatar() != null ? createdUser.getAvatar() : "",
                "phone", createdUser.getPhone() != null ? createdUser.getPhone() : "",
                "email", createdUser.getEmail() != null ? createdUser.getEmail() : "",
                "createTime", createdUser.getCreateTime() != null ? createdUser.getCreateTime().toString() : "",
                "updateTime", createdUser.getUpdateTime() != null ? createdUser.getUpdateTime().toString() : ""
            ));
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 管理员更新用户信息
     *
     * @param id   用户 ID
     * @param user 包含要更新的字段的用户对象
     * @return 更新结果
     */
    @PutMapping("/admin/update/{id}")
    public Map<String, Object> updateUserByAdmin(@PathVariable Long id, @RequestBody User user) {
        Map<String, Object> result = new HashMap<>();
        try {
            User updatedUser = userService.updateUserByAdmin(id, user);
            result.put("success", true);
            result.put("message", "更新成功");
            result.put("user", Map.of(
                "id", updatedUser.getId(),
                "username", updatedUser.getUsername(),
                "nickname", updatedUser.getNickname() != null ? updatedUser.getNickname() : "",
                "avatar", updatedUser.getAvatar() != null ? updatedUser.getAvatar() : "",
                "phone", updatedUser.getPhone() != null ? updatedUser.getPhone() : "",
                "email", updatedUser.getEmail() != null ? updatedUser.getEmail() : "",
                "createTime", updatedUser.getCreateTime() != null ? updatedUser.getCreateTime().toString() : "",
                "updateTime", updatedUser.getUpdateTime() != null ? updatedUser.getUpdateTime().toString() : ""
            ));
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 删除用户（管理员接口）
     *
     * @param id 用户 ID
     * @return 删除结果
     */
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteUser(@PathVariable Long id) {
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
