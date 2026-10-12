package com.afs.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 统一响应状态码枚举
 */
@Getter
@RequiredArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "参数错误"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统异常");

    private final int code;
    private final String message;
}
