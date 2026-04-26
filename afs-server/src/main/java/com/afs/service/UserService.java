package com.afs.service;

import com.afs.entity.User;
import com.afs.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户服务层
 *
 * 提供用户相关的业务逻辑，包括注册、登录、信息管理等功能。
 */
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 用户注册
     *
     * @param username 用户名
     * @param password 密码
     * @return 注册成功的用户对象
     * @throws RuntimeException 如果用户名已存在
     */
    public User register(String username, String password) {
        // 检查用户名是否已存在
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        if (userMapper.selectOne(wrapper) != null) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 创建新用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setNickname(username);  // 默认昵称与用户名相同
        user.setCreateTime(LocalDateTime.now());
        
        // 保存到数据库
        userMapper.insert(user);
        return user;
    }

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录成功的用户对象
     * @throws RuntimeException 如果用户名或密码错误
     */
    public User login(String username, String password) {
        // 构建登录查询条件
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username).eq("password", password);
        
        // 查询用户
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        return user;
    }

    /**
     * 根据 ID 获取用户
     *
     * @param id 用户 ID
     * @return 用户对象，如果不存在返回 null
     */
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    /**
     * 更新用户信息
     *
     * @param user 包含更新信息的用户对象
     * @return 更新后的用户对象
     */
    public User updateUser(User user) {
        // 设置更新时间
        user.setUpdateTime(LocalDateTime.now());
        // 更新到数据库
        userMapper.updateById(user);
        // 返回更新后的用户信息
        return userMapper.selectById(user.getId());
    }

    /**
     * 获取所有用户列表
     *
     * @return 按创建时间倒序排列的用户列表
     */
    public List<User> getAllUsers() {
        return userMapper.selectList(
                new LambdaQueryWrapper<User>().orderByDesc(User::getCreateTime)
        );
    }

    /**
     * 创建用户（管理员或内部使用）
     *
     * @param user 用户对象
     * @return 创建后的用户对象
     * @throws RuntimeException 如果用户名已存在
     */
    public User createUser(User user) {
        // 检查用户名是否已存在
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", user.getUsername());
        if (userMapper.selectOne(wrapper) != null) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 设置默认值
        if (user.getCreateTime() == null) {
            user.setCreateTime(LocalDateTime.now());
        }
        if (user.getNickname() == null || user.getNickname().isBlank()) {
            user.setNickname(user.getUsername());  // 默认昵称与用户名相同
        }
        
        // 保存到数据库
        userMapper.insert(user);
        return user;
    }

    /**
     * 管理员更新用户信息
     *
     * @param id  用户 ID
     * @param req 包含要更新的字段的用户对象
     * @return 更新后的用户对象
     * @throws RuntimeException 如果用户不存在
     */
    public User updateUserByAdmin(Long id, User req) {
        // 获取现有用户
        User existing = userMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 更新字段（只更新非空字段）
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
        
        // 设置更新时间
        existing.setUpdateTime(LocalDateTime.now());
        // 更新到数据库
        userMapper.updateById(existing);
        return existing;
    }

    /**
     * 删除用户
     *
     * @param id 用户 ID
     */
    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }
}
