package com.afs.exception;

import org.springframework.http.HttpStatus;

/**
 * 业务异常
 *
 * 用于处理业务逻辑中的各种异常情况，如参数验证失败、资源不存在等。
 */
public class BusinessException extends RuntimeException {

    private int code = HttpStatus.BAD_REQUEST.value();

    public BusinessException() {
        super();
    }

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(HttpStatus.NOT_FOUND.value(), message);
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST.value(), message);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(HttpStatus.UNAUTHORIZED.value(), message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(HttpStatus.FORBIDDEN.value(), message);
    }

    public static BusinessException serverError(String message) {
        return new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
    }
}