package com.afs.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * AuthInterceptor 单元测试
 *
 * 覆盖 preHandle 各分支：ASYNC 异步派发放行、OPTIONS 预检放行、
 * 合法 Bearer Token 放行并写入 userId、无 Token / 非法 Token /
 * 非 Bearer 头返回 401。
 */
class AuthInterceptorTest {

    private JwtUtil jwtUtil;
    private TokenBlacklistService tokenBlacklistService;
    private AuthInterceptor interceptor;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        tokenBlacklistService = mock(TokenBlacklistService.class);
        // 使用真实 ObjectMapper，避免对 JSON 序列化打桩
        interceptor = new AuthInterceptor(jwtUtil, tokenBlacklistService, new ObjectMapper());
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    @Test
    // SSE 异步派发请求直接放行，且不解析任何令牌
    void asyncDispatchPassesThroughWithoutAuth() throws Exception {
        when(request.getDispatcherType()).thenReturn(DispatcherType.ASYNC);

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
        verifyNoInteractions(jwtUtil);
    }

    @Test
    // CORS 预检 OPTIONS 请求直接放行
    void optionsRequestPassesThrough() throws Exception {
        when(request.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
        when(request.getMethod()).thenReturn("OPTIONS");

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
        verifyNoInteractions(jwtUtil);
    }

    @Test
    // 合法 Bearer Token：放行并把用户 ID 与角色写入请求属性
    void validBearerTokenSetsUserIdAndRoleAndPasses() throws Exception {
        when(request.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        // 打桩：合法令牌解析出用户 1001，角色 admin，且未吊销
        when(jwtUtil.parseUserId("valid-token")).thenReturn(1001L);
        when(jwtUtil.parseRole("valid-token")).thenReturn("admin");
        when(tokenBlacklistService.isRevoked("valid-token")).thenReturn(false);

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
        verify(request).setAttribute(AuthInterceptor.USER_ID_ATTRIBUTE, 1001L);
        verify(request).setAttribute(AuthInterceptor.ROLE_ATTRIBUTE, "admin");
    }

    @Test
    // 缺少 Authorization 头：返回 401 与统一错误体
    void missingAuthorizationHeaderReturns401() throws Exception {
        when(request.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(null);
        StringWriter body = stubResponseWriter();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(body.toString()).contains("未登录或登录已过期");
    }

    @Test
    // Token 非法（解析返回 null）：返回 401
    void invalidTokenReturns401() throws Exception {
        when(request.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn("Bearer bad-token");
        // 打桩：非法令牌解析结果为 null
        when(jwtUtil.parseUserId("bad-token")).thenReturn(null);
        StringWriter body = stubResponseWriter();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(body.toString()).contains("未登录或登录已过期");
    }

    @Test
    // 令牌已被吊销：返回 401
    void revokedTokenReturns401() throws Exception {
        when(request.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer revoked-token");
        when(jwtUtil.parseUserId("revoked-token")).thenReturn(1001L);
        when(tokenBlacklistService.isRevoked("revoked-token")).thenReturn(true);
        StringWriter body = stubResponseWriter();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(body.toString()).contains("未登录或登录已过期");
    }

    @Test
    // Authorization 头非 Bearer 前缀：不会尝试解析，直接 401
    void nonBearerAuthorizationHeaderReturns401() throws Exception {
        when(request.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");
        stubResponseWriter();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        verifyNoInteractions(jwtUtil);
    }

    /** 打桩 401 响应的状态码/内容类型/Writer，返回可断言的输出缓冲 */
    private StringWriter stubResponseWriter() throws Exception {
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));
        return body;
    }

    @Test
    // 401 分支会设置状态码与 JSON 内容类型
    void unauthorizedWritesStatusAndContentType() throws Exception {
        when(request.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(null);
        stubResponseWriter();

        interceptor.preHandle(request, response, new Object());

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        verify(response).setCharacterEncoding("UTF-8");
    }
}
