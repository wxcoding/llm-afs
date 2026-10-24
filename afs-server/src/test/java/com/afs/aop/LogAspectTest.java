package com.afs.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * LogAspect 单元测试
 *
 * 覆盖 around 切面的正常放行（原样返回业务结果）、
 * 异常透传（原样抛出且不吞异常），以及不同参数形态
 * （null 数组、空数组、null 元素、简单类型、复杂对象）下的执行路径。
 */
class LogAspectTest {

    private LogAspect aspect;
    private ProceedingJoinPoint joinPoint;
    private MethodSignature signature;

    @BeforeEach
    void setUp() throws Exception {
        aspect = new LogAspect();
        joinPoint = mock(ProceedingJoinPoint.class);
        signature = mock(MethodSignature.class);
        // 打桩：方法签名与目标对象，模拟 Controller 方法
        Method method = SampleController.class.getMethod("handle", String.class, int.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(new SampleController());
    }

    @Test
    // 正常执行：around 原样返回 proceed 的结果
    void aroundReturnsProceedResult() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{"query", 1});
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.around(joinPoint);

        assertThat(result).isEqualTo("ok");
    }

    @Test
    // 业务抛出异常：around 原样透传，不吞异常
    void aroundRethrowsProceedException() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        IllegalStateException boom = new IllegalStateException("boom");
        // 打桩：业务方法抛出运行时异常
        when(joinPoint.proceed()).thenThrow(boom);

        assertThatThrownBy(() -> aspect.around(joinPoint))
                .isSameAs(boom);
    }

    @Test
    // 参数为 null 数组时切面仍正常放行（getParams 的 null 分支）
    void aroundHandlesNullArgs() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(null);
        when(joinPoint.proceed()).thenReturn("done");

        assertThat(aspect.around(joinPoint)).isEqualTo("done");
    }

    @Test
    // 参数含 null 元素、布尔值与复杂对象时切面仍正常放行（getParams 各分支）
    void aroundHandlesMixedArgTypes() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{null, true, new Object()});
        when(joinPoint.proceed()).thenReturn(42);

        assertThat(aspect.around(joinPoint)).isEqualTo(42);
    }

    /** 模拟被切面拦截的 Controller */
    static class SampleController {
        public String handle(String name, int count) {
            return name + count;
        }
    }
}
