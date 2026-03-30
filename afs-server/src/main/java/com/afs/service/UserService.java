package com.afs.service;

import com.afs.entity.User;
import com.afs.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    public User register(String username, String password) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        if (userMapper.selectOne(wrapper) != null) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setNickname(username);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    public User login(String username, String password) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username).eq("password", password);
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
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
            throw new RuntimeException("用户名已存在");
        }
        if (user.getCreateTime() == null) {
            user.setCreateTime(LocalDateTime.now());
        }
        if (user.getNickname() == null || user.getNickname().isBlank()) {
            user.setNickname(user.getUsername());
        }
        userMapper.insert(user);
        return user;
    }

    public User updateUserByAdmin(Long id, User req) {
        User existing = userMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("用户不存在");
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
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            existing.setPassword(req.getPassword());
        }
        existing.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(existing);
        return existing;
    }

    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }
}
