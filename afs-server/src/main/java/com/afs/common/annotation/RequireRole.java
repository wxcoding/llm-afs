package com.afs.common.annotation;

import com.afs.common.constant.Roles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色准入注解
 *
 * 标注在 Controller 方法或类上，要求当前登录用户具备指定角色之一，
 * 否则由 RoleInterceptor 返回 403。方法上的注解优先于类上的注解。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /** 允许访问的角色列表，满足其一即可；默认仅管理员 */
    String[] value() default {Roles.ADMIN};
}
