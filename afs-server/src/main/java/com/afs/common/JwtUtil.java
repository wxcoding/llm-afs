package com.afs.common;

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
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expireHours;

    public JwtUtil(@Value("${jwt.secret:afs-jwt-secret-please-change-in-production-0123456789}") String secret,
                   @Value("${jwt.expire-hours:72}") long expireHours) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireHours = expireHours;
    }

    /**
     * 为指定用户签发令牌
     *
     * @param userId 用户 ID
     * @return 签名的 JWT 字符串
     */
    public String generateToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
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
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.valueOf(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
