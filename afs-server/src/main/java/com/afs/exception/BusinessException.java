package com.afs.exception;

import org.springframework.http.HttpStatus;

import java.util.Objects;

/**
 * 用于表示可预期的业务错误。
 *
 * @author guanwx
 */
public class BusinessException extends RuntimeException {

    /** 业务错误对应的 HTTP 状态码 */
    private final HttpStatus status;

    /**
     * 构造业务异常（HTTP 状态默认 400）
     *
     * @param message 错误信息
     */
    public BusinessException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * 构造业务异常（HTTP 状态默认 400），携带原因异常
     *
     * @param message 错误信息
     * @param cause   原因异常
     */
    public BusinessException(String message, Throwable cause) {
        this(HttpStatus.BAD_REQUEST, message, cause);
    }

    /**
     * 构造业务异常
     *
     * @param status  HTTP 状态
     * @param message 错误信息
     */
    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    /**
     * 构造业务异常，携带原因异常
     *
     * @param status  HTTP 状态
     * @param message 错误信息
     * @param cause   原因异常
     */
    public BusinessException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    /**
     * 获取异常对应的 HTTP 状态
     *
     * @return HTTP 状态
     */
    public HttpStatus getStatus() {
        return status;
    }

    /**
     * 创建 404 业务异常
     *
     * @param message 错误信息
     * @return 业务异常
     */
    public static BusinessException notFound(String message) {
        return new BusinessException(HttpStatus.NOT_FOUND, message);
    }

    /**
     * 创建 400 业务异常
     *
     * @param message 错误信息
     * @return 业务异常
     */
    public static BusinessException badRequest(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * 创建 401 业务异常
     *
     * @param message 错误信息
     * @return 业务异常
     */
    public static BusinessException unauthorized(String message) {
        return new BusinessException(HttpStatus.UNAUTHORIZED, message);
    }

    /**
     * 创建 403 业务异常
     *
     * @param message 错误信息
     * @return 业务异常
     */
    public static BusinessException forbidden(String message) {
        return new BusinessException(HttpStatus.FORBIDDEN, message);
    }

    /**
     * 创建 500 业务异常
     *
     * @param message 错误信息
     * @return 业务异常
     */
    public static BusinessException serverError(String message) {
        return new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}