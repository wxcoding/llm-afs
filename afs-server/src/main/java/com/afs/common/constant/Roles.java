package com.afs.common.constant;

/**
 * RBAC 角色常量
 *
 * 角色以字符串形式存储于 user 表的 role 字段，
 * 并通过 JWT claim 随令牌下发，避免每次请求查库。
 */
public final class Roles {

    /** 普通用户 */
    public static final String USER = "user";

    /** 管理员 */
    public static final String ADMIN = "admin";

    private Roles() {
    }
}
