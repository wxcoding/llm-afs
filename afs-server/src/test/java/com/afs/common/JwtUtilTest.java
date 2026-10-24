package com.afs.common;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtUtil 单元测试
 *
 * 覆盖令牌签发、解析回环、非法令牌、过期令牌、签名不匹配、
 * 非数字 subject 等分支。
 * 注：JwtUtil 的 secret/过期时间通过构造器注入（final 字段），
 * 测试直接使用构造器传入，无需 ReflectionTestUtils 设值。
 */
class JwtUtilTest {

    /** 测试使用的密钥，长度足够满足 HMAC-SHA 要求 */
    private static final String SECRET = "test-jwt-secret-key-for-unit-test-0123456789abcdef";

    @Test
    // 签发后可以解析回相同的用户 ID
    void generateTokenThenParseReturnsSameUserId() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 72);

        String token = jwtUtil.generateToken(42L);

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.parseUserId(token)).isEqualTo(42L);
    }

    @Test
    // 解析 null / 空串 / 乱码令牌均返回 null
    void parseUserIdReturnsNullForNullEmptyAndGarbageToken() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 72);

        assertThat(jwtUtil.parseUserId(null)).isNull();
        assertThat(jwtUtil.parseUserId("")).isNull();
        assertThat(jwtUtil.parseUserId("not-a-jwt-token")).isNull();
    }

    @Test
    // 使用不同密钥签发的令牌无法通过校验，返回 null
    void parseUserIdReturnsNullForTokenSignedWithDifferentSecret() {
        JwtUtil issuer = new JwtUtil(SECRET, 72);
        JwtUtil verifier = new JwtUtil("another-secret-key-totally-different-0123456789", 72);

        String token = issuer.generateToken(1L);

        assertThat(verifier.parseUserId(token)).isNull();
    }

    @Test
    // 过期时间为负数时令牌签发即过期，解析返回 null
    void parseUserIdReturnsNullForExpiredToken() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, -1);

        String token = jwtUtil.generateToken(7L);

        assertThat(jwtUtil.parseUserId(token)).isNull();
    }

    @Test
    // subject 非数字时触发 NumberFormatException（IllegalArgumentException 子类），返回 null
    void parseUserIdReturnsNullForNonNumericSubject() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 72);
        // 通过反射取出内部密钥，手工构造一个 subject 非数字的合法签名令牌
        SecretKey key = (SecretKey) ReflectionTestUtils.getField(jwtUtil, "secretKey");
        assertThat(key).isNotNull();

        String token = Jwts.builder()
                .subject("abc")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600_000L))
                .signWith(key)
                .compact();

        assertThat(jwtUtil.parseUserId(token)).isNull();
    }
}
