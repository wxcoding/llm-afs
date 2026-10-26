package com.afs.security;

import com.afs.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 登录防爆破限流器（Redis）
 *
 * 同一用户名连续登录失败达到阈值后锁定一段时间，锁定窗口内直接拒绝登录尝试，
 * 登录成功立即清零。失败计数键带 TTL，窗口到期自动解锁。
 * Redis 不可用时降级放行（fail-open）并记录告警。
 *
 * @author guanwx
 */
@Slf4j
@Service
public class LoginRateLimiter {

    /** Redis key 前缀，value 为窗口内失败次数 */
    private static final String KEY_PREFIX = "login:fail:";

    /** Redis 字符串模板，用于失败计数的读写 */
    private final StringRedisTemplate redisTemplate;
    /** 窗口内允许的最大失败次数 */
    private final int maxFailures;
    /** 锁定时长（秒） */
    private final long lockSeconds;

    /**
     * 构造登录限流器
     *
     * @param redisTemplate Redis 字符串模板
     * @param maxFailures   窗口内允许的最大失败次数
     * @param lockSeconds   锁定时长（秒）
     */
    public LoginRateLimiter(StringRedisTemplate redisTemplate,
                            @Value("${security.login.max-failures:5}") int maxFailures,
                            @Value("${security.login.lock-seconds:600}") long lockSeconds) {
        this.redisTemplate = redisTemplate;
        this.maxFailures = maxFailures;
        this.lockSeconds = lockSeconds;
    }

    /**
     * 登录前检查：失败次数已达上限时抛出 429 业务异常
     *
     * @param username 登录用户名
     * @throws BusinessException 锁定中（HTTP 429）
     */
    public void checkLocked(String username) {
        Long failures = getFailures(username);
        if (failures != null && failures >= maxFailures) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS,
                    "登录失败次数过多，账号已临时锁定，请稍后再试");
        }
    }

    /**
     * 记录一次登录失败；窗口内首次失败时设置 TTL
     *
     * @param username 登录用户名
     */
    public void onFailure(String username) {
        if (username == null) {
            return;
        }
        String key = KEY_PREFIX + username;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, Duration.ofSeconds(lockSeconds));
            }
        } catch (DataAccessException e) {
            log.warn("Redis 不可用，登录失败计数跳过（fail-open）: {}", e.getMessage());
        }
    }

    /**
     * 登录成功：清除失败计数
     *
     * @param username 登录用户名
     */
    public void onSuccess(String username) {
        if (username == null) {
            return;
        }
        try {
            redisTemplate.delete(KEY_PREFIX + username);
        } catch (DataAccessException e) {
            log.warn("Redis 不可用，登录失败计数清除跳过（fail-open）: {}", e.getMessage());
        }
    }

    /** 读取当前窗口失败次数；Redis 不可用时返回 null（fail-open，视为未锁定） */
    private Long getFailures(String username) {
        if (username == null) {
            return null;
        }
        try {
            String value = redisTemplate.opsForValue().get(KEY_PREFIX + username);
            return value == null ? null : Long.parseLong(value);
        } catch (DataAccessException e) {
            log.warn("Redis 不可用，登录锁定检查跳过（fail-open）: {}", e.getMessage());
            return null;
        }
    }
}
