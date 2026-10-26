package com.afs.security;

import com.afs.common.Result;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 登录鉴权拦截器
 *
 * 校验请求头中的 Bearer Token，解析出用户 ID 写入请求属性，
 * 后续 Controller 通过 request.getAttribute("userId") 获取当前用户。
 */
@RequiredArgsConstructor
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USER_ID_ATTRIBUTE = "userId";

    /** 当前登录用户角色在请求属性中的键，由 RoleInterceptor 消费 */
    public static final String ROLE_ATTRIBUTE = "role";

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // SSE 异步派发请求已在初始请求完成鉴权，直接放行
        if (DispatcherType.ASYNC.equals(request.getDispatcherType())) {
            return true;
        }
        // CORS 预检请求不携带业务头，直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            Long userId = jwtUtil.parseUserId(token);
            // 已吊销（登出）的令牌视为未登录
            if (userId != null && !tokenBlacklistService.isRevoked(token)) {
                request.setAttribute(USER_ID_ATTRIBUTE, userId);
                request.setAttribute(ROLE_ATTRIBUTE, jwtUtil.parseRole(token));
                return true;
            }
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.unauthorized("未登录或登录已过期")));
        return false;
    }
}
