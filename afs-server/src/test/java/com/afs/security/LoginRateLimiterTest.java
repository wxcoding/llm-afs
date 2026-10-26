package com.afs.security;

import com.afs.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LoginRateLimiter 单元测试
 *
 * 覆盖锁定阈值判断（5 次失败锁定）、失败计数递增与首次失败设置 TTL、
 * 登录成功清零，以及 Redis 不可用时的 fail-open 降级行为。
 */
@ExtendWith(MockitoExtension.class)
class LoginRateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    /** 阈值 5、锁窗 600 秒，与生产默认值一致 */
    private LoginRateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new LoginRateLimiter(redisTemplate, 5, 600L);
    }

    /** 未达阈值：4 次失败仍放行 */
    @Test
    void checkLocked_passesWhenBelowThreshold() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("login:fail:alice")).thenReturn("4");

        limiter.checkLocked("alice"); // 不应抛出异常
    }

    /** 达到阈值：5 次失败抛出 429 业务异常 */
    @Test
    void checkLocked_throws429WhenThresholdReached() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("login:fail:alice")).thenReturn("5");

        assertThatThrownBy(() -> limiter.checkLocked("alice"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("登录失败次数过多")
                .satisfies(e -> assertThat(((BusinessException) e).getStatus())
                        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    /** 无失败记录：直接放行 */
    @Test
    void checkLocked_passesWhenNoRecord() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("login:fail:alice")).thenReturn(null);

        limiter.checkLocked("alice"); // 不应抛出异常
    }

    /** 记录失败：首次失败递增计数并设置锁窗 TTL */
    @Test
    void onFailure_incrementsAndSetsTtlOnFirstFailure() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("login:fail:alice")).thenReturn(1L);

        limiter.onFailure("alice");

        verify(valueOperations).increment("login:fail:alice");
        verify(redisTemplate).expire(eq("login:fail:alice"), eq(Duration.ofSeconds(600)));
    }

    /** 记录失败：非首次失败不重复设置 TTL（避免窗口被续期） */
    @Test
    void onFailure_doesNotRefreshTtlOnSubsequentFailures() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("login:fail:alice")).thenReturn(3L);

        limiter.onFailure("alice");

        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    /** 记录失败：空用户名不访问 Redis */
    @Test
    void onFailure_skipsWhenUsernameNull() {
        limiter.onFailure(null);

        verify(redisTemplate, never()).opsForValue();
    }

    /** 登录成功：删除失败计数键 */
    @Test
    void onSuccess_deletesCounterKey() {
        limiter.onSuccess("alice");

        verify(redisTemplate).delete("login:fail:alice");
    }

    /** 登录成功：空用户名不访问 Redis */
    @Test
    void onSuccess_skipsWhenUsernameNull() {
        limiter.onSuccess(null);

        verify(redisTemplate, never()).delete(anyString());
    }

    /** Redis 不可用：锁定检查 fail-open 放行，失败计数静默跳过 */
    @Test
    void redisDown_failsOpen() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString()))
                .thenThrow(new DataAccessResourceFailureException("connection refused"));
        when(valueOperations.increment(anyString()))
                .thenThrow(new DataAccessResourceFailureException("connection refused"));
        when(redisTemplate.delete(anyString()))
                .thenThrow(new DataAccessResourceFailureException("connection refused"));

        // 三个入口均不应抛出异常
        limiter.checkLocked("alice");
        limiter.onFailure("alice");
        limiter.onSuccess("alice");
    }
}
