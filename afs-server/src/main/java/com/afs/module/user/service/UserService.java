package com.afs.module.user.service;

import com.afs.common.constant.Roles;
import com.afs.exception.BusinessException;
import com.afs.module.user.entity.User;
import com.afs.module.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户服务类
 *
 * 负责用户注册、登录、个人信息管理、管理员用户管理等功能。
 * 密码统一使用 BCrypt 加密存储；历史明文密码在登录成功时自动升级。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {

    /** 默认头像路径（前端 public 目录下，浏览器可直接访问） */
    private static final String DEFAULT_AVATAR = "/avatars/default.jpg";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 用户注册
     * 
     * @param username 用户名
     * @param password 密码
     * @return 注册成功的用户对象
     * @throws RuntimeException 用户名已存在时抛出异常
     */
    public User register(String username, String password) {
        // 检查用户名是否已存在
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        if (userMapper.selectOne(wrapper) != null) {
            throw BusinessException.badRequest("用户名已存在");
        }
        
        // 创建新用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(username);
        user.setAvatar(DEFAULT_AVATAR);
        user.setRole(Roles.USER);
        user.setCreateTime(LocalDateTime.now());

        userMapper.insert(user);
        return user;
    }

    /**
     * 用户登录
     *
     * 历史明文密码登录成功后自动升级为 BCrypt 加密存储。
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录成功的用户对象
     * @throws RuntimeException 用户名或密码错误时抛出异常
     */
    public User login(String username, String password) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);

        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw BusinessException.unauthorized("用户名或密码错误");
        }

        String stored = user.getPassword();
        if (stored != null && stored.startsWith("$2")) {
            // BCrypt 校验
            if (!passwordEncoder.matches(password, stored)) {
                throw BusinessException.unauthorized("用户名或密码错误");
            }
        } else {
            // 兼容存量明文密码，校验通过后升级为 BCrypt
            if (!password.equals(stored)) {
                throw BusinessException.unauthorized("用户名或密码错误");
            }
            user.setPassword(passwordEncoder.encode(password));
            user.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(user);
            log.info("用户 {} 的明文密码已升级为 BCrypt 加密存储", username);
        }
        return user;
    }

    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    public User updateUser(User user) {
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return userMapper.selectById(user.getId());
    }

    public List<User> getAllUsers() {
        return userMapper.selectList(
                new LambdaQueryWrapper<User>().orderByDesc(User::getCreateTime)
        );
    }

    public User createUser(User user) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", user.getUsername());
        if (userMapper.selectOne(wrapper) != null) {
            throw BusinessException.badRequest("用户名已存在");
        }
        
        if (user.getCreateTime() == null) {
            user.setCreateTime(LocalDateTime.now());
        }
        if (user.getNickname() == null || user.getNickname().isBlank()) {
            user.setNickname(user.getUsername());
        }
        if (user.getAvatar() == null || user.getAvatar().isBlank()) {
            user.setAvatar(DEFAULT_AVATAR);
        }
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole(Roles.USER);
        } else if (!Roles.USER.equals(user.getRole()) && !Roles.ADMIN.equals(user.getRole())) {
            throw BusinessException.badRequest("非法角色: " + user.getRole());
        }
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        userMapper.insert(user);
        return user;
    }

    public User updateUserByAdmin(Long id, User req) {
        User existing = userMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.notFound("用户不存在");
        }
        
        if (req.getNickname() != null) {
            existing.setNickname(req.getNickname());
        }
        if (req.getPhone() != null) {
            existing.setPhone(req.getPhone());
        }
        if (req.getEmail() != null) {
            existing.setEmail(req.getEmail());
        }
        if (req.getAvatar() != null) {
            existing.setAvatar(req.getAvatar());
        }
        if (req.getRole() != null && !req.getRole().isBlank()) {
            if (!Roles.USER.equals(req.getRole()) && !Roles.ADMIN.equals(req.getRole())) {
                throw BusinessException.badRequest("非法角色: " + req.getRole());
            }
            existing.setRole(req.getRole());
        }
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        
        existing.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(existing);
        return existing;
    }

    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }
}
