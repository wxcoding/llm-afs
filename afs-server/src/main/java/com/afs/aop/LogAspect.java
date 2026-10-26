package com.afs.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 操作日志切面
 *
 * 记录所有 Controller 方法的执行时间、参数和结果。
 *
 * @author guanwx
 */
@Slf4j
@Aspect
@Component
public class LogAspect {

    /**
     * 切点：匹配所有 Controller 方法
     */
    @Pointcut("execution(* com.afs.module..controller..*.*(..))")
    public void controllerPointcut() {
    }

    /**
     * 环绕通知：记录方法入参、执行耗时与执行结果（成功/失败）
     *
     * @param joinPoint 连接点
     * @return 目标方法的返回值
     * @throws Throwable 目标方法抛出的异常
     */
    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = method.getName();

        Object[] args = joinPoint.getArgs();
        String params = getParams(args);

        log.info("[操作日志] {}.{} 开始 | 参数: {}", className, methodName, params);

        Object result = null;
        Throwable error = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            long costTime = System.currentTimeMillis() - startTime;
            if (error != null) {
                log.error("[操作日志] {}.{} 失败 | 耗时: {}ms | 错误: {}",
                        className, methodName, costTime, error.getMessage());
            } else {
                log.info("[操作日志] {}.{} 完成 | 耗时: {}ms", className, methodName, costTime);
            }
        }
    }

    /**
     * 将方法参数格式化为字符串：简单类型输出值，复杂类型输出类名
     *
     * @param args 方法参数数组
     * @return 格式化后的参数字符串
     */
    private String getParams(Object[] args) {
        if (args == null || args.length == 0) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{");

        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg == null) {
                sb.append("null");
            } else if (isSimpleType(arg)) {
                sb.append(String.valueOf(arg));
            } else {
                sb.append(arg.getClass().getSimpleName());
            }

            if (i < args.length - 1) {
                sb.append(", ");
            }
        }

        sb.append("}");
        return sb.toString();
    }

    /**
     * 判断对象是否为简单类型（String/数字/布尔/字符）
     *
     * @param obj 待判断对象
     * @return true 表示简单类型
     */
    private boolean isSimpleType(Object obj) {
        return obj instanceof String ||
               obj instanceof Number ||
               obj instanceof Boolean ||
               obj instanceof Character;
    }
}