package com.afs.controller;

import com.afs.entity.User;
import com.afs.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@CrossOrigin
public class UserController {

    @Autowired
    private UserService userService;

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
                    "email", user.getEmail() != null ? user.getEmail() : ""
                ));
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

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
                    "email", updatedUser.getEmail() != null ? updatedUser.getEmail() : ""
                ));
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

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
                "createTime", user.getCreateTime() != null ? user.getCreateTime().toString() : ""
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
                "email", createdUser.getEmail() != null ? createdUser.getEmail() : ""
            ));
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

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
                "email", updatedUser.getEmail() != null ? updatedUser.getEmail() : ""
            ));
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

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
