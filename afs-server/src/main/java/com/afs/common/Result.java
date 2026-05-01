package com.afs.common;

import org.springframework.http.HttpStatus;

/**
 * 统一响应结果封装
 *
 * 所有 API 响应都使用此格式返回，确保前端处理一致。
 *
 * @param <T> 响应数据类型
 */
public class Result<T> {

    private boolean success;
    private String message;
    private T data;
    private int code;
    private long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public Result(boolean success, String message, T data, int code) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.code = code;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> Result<T> success() {
        return new Result<>(true, "操作成功", null, HttpStatus.OK.value());
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(true, "操作成功", data, HttpStatus.OK.value());
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(true, message, data, HttpStatus.OK.value());
    }

    public static <T> Result<T> success(String message) {
        return new Result<>(true, message, null, HttpStatus.OK.value());
    }

    public static <T> Result<T> error() {
        return new Result<>(false, "操作失败", null, HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(false, message, null, HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(false, message, null, code);
    }

    public static <T> Result<T> error(HttpStatus status, String message) {
        return new Result<>(false, message, null, status.value());
    }

    public static <T> Result<T> badRequest(String message) {
        return new Result<>(false, message, null, HttpStatus.BAD_REQUEST.value());
    }

    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(false, message, null, HttpStatus.UNAUTHORIZED.value());
    }

    public static <T> Result<T> forbidden(String message) {
        return new Result<>(false, message, null, HttpStatus.FORBIDDEN.value());
    }

    public static <T> Result<T> notFound(String message) {
        return new Result<>(false, message, null, HttpStatus.NOT_FOUND.value());
    }

    public boolean isSuccess() {
        return success;
    }

    public Result<T> setSuccess(boolean success) {
        this.success = success;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public Result<T> setMessage(String message) {
        this.message = message;
        return this;
    }

    public T getData() {
        return data;
    }

    public Result<T> setData(T data) {
        this.data = data;
        return this;
    }

    public int getCode() {
        return code;
    }

    public Result<T> setCode(int code) {
        this.code = code;
        return this;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Result<T> setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }
}