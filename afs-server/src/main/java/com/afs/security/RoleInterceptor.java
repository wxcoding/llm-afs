package com.afs.security;

import com.afs.common.Result;
import com.afs.common.annotation.RequireRole;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * RBAC 角色校验拦截器
 *
 * 在 AuthInterceptor 之后执行，读取 @RequireRole 注解（方法优先于类），
 * 将当前用户角色（AuthInterceptor 写入的请求属性）与允许角色比对，
 * 不匹配时返回 403。未标注 @RequireRole 的接口只需登录即可访问。
 */
@RequiredArgsConstructor
@Component
public class RoleInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // SSE 异步派发放行（与 AuthInterceptor 保持一致）
        if (DispatcherType.ASYNC.equals(request.getDispatcherType())) {
            return true;
        }
        // 非 Controller 方法（如静态资源）不做角色校验
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireRole requireRole = resolveRequireRole(handlerMethod);
        if (requireRole == null) {
            return true;
        }

        Object role = request.getAttribute(AuthInterceptor.ROLE_ATTRIBUTE);
        if (role instanceof String roleStr && Arrays.asList(requireRole.value()).contains(roleStr)) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.forbidden("无权限访问")));
        return false;
    }

    /** 方法级注解优先，其次类级注解 */
    private RequireRole resolveRequireRole(HandlerMethod handlerMethod) {
        RequireRole annotation = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (annotation == null) {
            annotation = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        return annotation;
    }
}
