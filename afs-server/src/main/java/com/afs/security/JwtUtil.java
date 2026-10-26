package com.afs.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 令牌工具
 *
 * 负责登录令牌的签发与解析，令牌载荷仅包含用户 ID 与过期时间。
 *
 * @author guanwx
 */
@Component
public class JwtUtil {

    /** HMAC 签名密钥 */
    private final SecretKey secretKey;
    /** 令牌有效期（小时） */
    private final long expireHours;

    /**
     * 构造 JWT 工具
     *
     * @param secret      HMAC 签名密钥
     * @param expireHours 令牌有效期（小时）
     */
    public JwtUtil(@Value("${jwt.secret:afs-jwt-secret-please-change-in-production-0123456789}") String secret,
                   @Value("${jwt.expire-hours:72}") long expireHours) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireHours = expireHours;
    }

    /**
     * 为指定用户签发令牌，角色随令牌下发以支持 RBAC 校验
     *
     * @param userId 用户 ID
     * @param role   用户角色（见 {@link Roles}）
     * @return 签名的 JWT 字符串
     */
    public String generateToken(Long userId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireHours * 3600_000L))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析令牌并返回用户 ID
     *
     * @param token JWT 字符串
     * @return 用户 ID；令牌无效或已过期时返回 null
     */
    public Long parseUserId(String token) {
        try {
            Claims claims = parseClaims(token);
            return Long.valueOf(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 解析令牌中的角色 claim
     *
     * @param token JWT 字符串
     * @return 角色字符串；令牌无效、已过期或无角色 claim 时返回 null
     */
    public String parseRole(String token) {
        try {
            return parseClaims(token).get("role", String.class);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 解析令牌载荷
     *
     * @param token JWT 字符串
     * @return 令牌 Claims
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 计算令牌剩余有效期（秒），用于登出吊销时设置黑名单 TTL
     *
     * @param token JWT 字符串
     * @return 剩余秒数；令牌无效或已过期时返回 0
     */
    public long getRemainingSeconds(String token) {
        try {
            Date expiration = parseClaims(token).getExpiration();
            long remaining = (expiration.getTime() - System.currentTimeMillis()) / 1000;
            return Math.max(remaining, 0);
        } catch (JwtException | IllegalArgumentException e) {
            return 0;
        }
    }
}
