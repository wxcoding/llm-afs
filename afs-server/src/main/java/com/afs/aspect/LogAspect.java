package com.afs.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 操作日志切面
 *
 * 记录所有 Controller 方法的执行时间、参数和结果。
 */
@Aspect
@Component
public class LogAspect {

    private static final Logger logger = LoggerFactory.getLogger(LogAspect.class);

    @Pointcut("execution(* com.afs.module..controller..*.*(..))")
    public void controllerPointcut() {
    }

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = method.getName();

        Object[] args = joinPoint.getArgs();
        String params = getParams(args);

        logger.info("[操作日志] {}.{} 开始 | 参数: {}", className, methodName, params);

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
                logger.error("[操作日志] {}.{} 失败 | 耗时: {}ms | 错误: {}",
                        className, methodName, costTime, error.getMessage());
            } else {
                logger.info("[操作日志] {}.{} 完成 | 耗时: {}ms", className, methodName, costTime);
            }
        }
    }

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

    private boolean isSimpleType(Object obj) {
        return obj instanceof String ||
               obj instanceof Number ||
               obj instanceof Boolean ||
               obj instanceof Character;
    }
}