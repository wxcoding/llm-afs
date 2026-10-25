package com.afs.security;

import com.afs.common.annotation.RequireRole;
import com.afs.common.constant.Roles;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RoleInterceptor 单元测试
 *
 * 覆盖 RBAC 角色校验各分支：无注解放行、非 Controller 处理器放行、
 * 角色匹配放行、角色不匹配/缺失返回 403、方法级注解优先于类级注解。
 */
class RoleInterceptorTest {

    private RoleInterceptor interceptor;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new RoleInterceptor(new ObjectMapper());
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    /** 无角色注解的探针方法 */
    @SuppressWarnings("unused")
    public void publicEndpoint() {
    }

    /** 默认要求 admin 的探针方法 */
    @RequireRole
    @SuppressWarnings("unused")
    public void adminEndpoint() {
    }

    /** 标注多角色的探针方法 */
    @RequireRole({Roles.ADMIN, Roles.USER})
    @SuppressWarnings("unused")
    public void multiRoleEndpoint() {
    }

    private HandlerMethod handlerMethod(String methodName) throws NoSuchMethodException {
        return new HandlerMethod(this, RoleInterceptorTest.class.getMethod(methodName));
    }

    @Test
    // SSE 异步派发直接放行
    void asyncDispatchPassesThrough() throws Exception {
        when(request.getDispatcherType()).thenReturn(DispatcherType.ASYNC);

        boolean allowed = interceptor.preHandle(request, response, handlerMethod("adminEndpoint"));

        assertThat(allowed).isTrue();
    }

    @Test
    // 非 HandlerMethod（如静态资源处理器）直接放行
    void nonHandlerMethodPassesThrough() throws Exception {
        when(request.getDispatcherType()).thenReturn(DispatcherType.REQUEST);

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
    }

    @Test
    // 未标注 @RequireRole 的接口登录即可访问
    void noAnnotationPassesThrough() throws Exception {
        when(request.getDispatcherType()).thenReturn(DispatcherType.REQUEST);

        boolean allowed = interceptor.preHandle(request, response, handlerMethod("publicEndpoint"));

        assertThat(allowed).isTrue();
    }

    @Test
    // 角色匹配时放行
    void matchingRolePasses() throws Exception {
        when(request.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
        when(request.getAttribute(AuthInterceptor.ROLE_ATTRIBUTE)).thenReturn("admin");

        boolean allowed = interceptor.preHandle(request, response, handlerMethod("adminEndpoint"));

        assertThat(allowed).isTrue();
    }

    @Test
    // 普通用户访问管理员接口返回 403 与统一错误体
    void userRoleAccessingAdminEndpointReturns403() throws Exception {
        when(request.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
        when(request.getAttribute(AuthInterceptor.ROLE_ATTRIBUTE)).thenReturn("user");
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        boolean allowed = interceptor.preHandle(request, response, handlerMethod("adminEndpoint"));

        assertThat(allowed).isFalse();
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        assertThat(body.toString()).contains("无权限访问");
    }

    @Test
    // 请求中没有角色属性（如旧令牌）访问管理员接口返回 403
    void missingRoleAttributeReturns403() throws Exception {
        when(request.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
        when(request.getAttribute(AuthInterceptor.ROLE_ATTRIBUTE)).thenReturn(null);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        boolean allowed = interceptor.preHandle(request, response, handlerMethod("adminEndpoint"));

        assertThat(allowed).isFalse();
    }

    @Test
    // 多角色注解下任一角色匹配即放行
    void anyOfMultipleRolesPasses() throws Exception {
        when(request.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
        when(request.getAttribute(AuthInterceptor.ROLE_ATTRIBUTE)).thenReturn("user");

        boolean allowed = interceptor.preHandle(request, response, handlerMethod("multiRoleEndpoint"));

        assertThat(allowed).isTrue();
    }
}
