package com.afs.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * 令牌吊销服务（Redis 黑名单）
 *
 * 登出时将令牌的 SHA-256 摘要写入黑名单，TTL 与令牌剩余有效期一致，
 * 拦截器命中黑名单即视为未登录。Redis 不可用时降级放行（fail-open）
 * 并记录告警，避免缓存故障拖垮全部受保护接口。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class TokenBlacklistService {

    /** Redis key 前缀，value 恒为 "1"，存在即代表已吊销 */
    private static final String KEY_PREFIX = "token:blacklist:";

    private final StringRedisTemplate redisTemplate;

    /**
     * 吊销令牌
     *
     * @param token      JWT 原文
     * @param ttlSeconds 令牌剩余有效期（秒）；<=0 时无需写入
     */
    public void revoke(String token, long ttlSeconds) {
        if (token == null || ttlSeconds <= 0) {
            return;
        }
        try {
            redisTemplate.opsForValue()
                    .set(KEY_PREFIX + sha256(token), "1", Duration.ofSeconds(ttlSeconds));
        } catch (DataAccessException e) {
            log.warn("Redis 不可用，令牌吊销失败（fail-open）: {}", e.getMessage());
        }
    }

    /**
     * 判断令牌是否已被吊销；Redis 不可用时返回 false（fail-open）
     *
     * @param token JWT 原文
     * @return true 表示已吊销
     */
    public boolean isRevoked(String token) {
        if (token == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + sha256(token)));
        } catch (DataAccessException e) {
            log.warn("Redis 不可用，跳过黑名单校验（fail-open）: {}", e.getMessage());
            return false;
        }
    }

    /** 对令牌做 SHA-256，避免在 Redis key 中存放令牌敏感原文 */
    private String sha256(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }
}
