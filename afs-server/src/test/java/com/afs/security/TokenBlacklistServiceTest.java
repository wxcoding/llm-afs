package com.afs.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TokenBlacklistService 单元测试
 *
 * 覆盖令牌吊销写入、黑名单命中判断、空令牌/非正 TTL 的跳过逻辑，
 * 以及 Redis 不可用时的 fail-open 降级行为。
 */
@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private TokenBlacklistService service;

    @BeforeEach
    void setUp() {
        service = new TokenBlacklistService(redisTemplate);
    }

    /** 吊销：以 SHA-256 摘要为 key 写入 Redis，TTL 与入参一致，key 不含令牌原文 */
    @Test
    void revoke_writesHashedKeyWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service.revoke("jwt-token-abc", 3600L);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(keyCaptor.capture(), eq("1"), ttlCaptor.capture());
        assertThat(keyCaptor.getValue())
                .startsWith("token:blacklist:")
                .doesNotContain("jwt-token-abc");
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofSeconds(3600));
    }

    /** 吊销：TTL 非正时不写入 Redis */
    @Test
    void revoke_skipsWhenTtlNotPositive() {
        service.revoke("jwt-token-abc", 0);
        service.revoke("jwt-token-abc", -5);

        verify(redisTemplate, never()).opsForValue();
    }

    /** 吊销：空令牌不写入 Redis */
    @Test
    void revoke_skipsWhenTokenNull() {
        service.revoke(null, 3600L);

        verify(redisTemplate, never()).opsForValue();
    }

    /** 吊销：Redis 不可用时静默降级，不向外抛异常 */
    @Test
    void revoke_failsOpenWhenRedisDown() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        org.mockito.Mockito.doThrow(new DataAccessResourceFailureException("connection refused"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        // 不应抛出异常
        service.revoke("jwt-token-abc", 3600L);
    }

    /** 命中判断：黑名单中存在时返回 true */
    @Test
    void isRevoked_returnsTrueWhenKeyExists() {
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        assertThat(service.isRevoked("jwt-token-abc")).isTrue();
    }

    /** 命中判断：黑名单中不存在时返回 false */
    @Test
    void isRevoked_returnsFalseWhenKeyMissing() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        assertThat(service.isRevoked("jwt-token-abc")).isFalse();
    }

    /** 命中判断：空令牌直接返回 false，不访问 Redis */
    @Test
    void isRevoked_returnsFalseWhenTokenNull() {
        assertThat(service.isRevoked(null)).isFalse();

        verify(redisTemplate, never()).hasKey(anyString());
    }

    /** 命中判断：Redis 不可用时 fail-open 返回 false */
    @Test
    void isRevoked_failsOpenWhenRedisDown() {
        when(redisTemplate.hasKey(anyString()))
                .thenThrow(new DataAccessResourceFailureException("connection refused"));

        assertThat(service.isRevoked("jwt-token-abc")).isFalse();
    }
}
