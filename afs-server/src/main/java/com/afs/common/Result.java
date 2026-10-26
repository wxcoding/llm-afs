package com.afs.common;

import lombok.Data;
import org.springframework.http.HttpStatus;

/**
 * 统一响应结果封装
 *
 * 所有 API 响应都使用此格式返回，确保前端处理一致。
 *
 * @param <T> 响应数据类型
 * @author guanwx
 */
@Data
public class Result<T> {

    /** 是否成功 */
    private boolean success;
    /** 提示信息 */
    private String message;
    /** 响应数据 */
    private T data;
    /** 状态码 */
    private int code;
    /** 响应时间戳 */
    private long timestamp;

    /**
     * 默认构造方法，自动设置当前时间戳
     */
    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 全参构造方法
     *
     * @param success 是否成功
     * @param message 提示信息
     * @param data 响应数据
     * @param code 状态码
     */
    public Result(boolean success, String message, T data, int code) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.code = code;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 构造无数据的成功响应
     *
     * @param <T> 响应数据类型
     * @return 成功结果
     */
    public static <T> Result<T> success() {
        return new Result<>(true, "操作成功", null, HttpStatus.OK.value());
    }

    /**
     * 构造带数据的成功响应
     *
     * @param data 响应数据
     * @param <T> 响应数据类型
     * @return 成功结果
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(true, "操作成功", data, HttpStatus.OK.value());
    }

    /**
     * 构造自定义消息并带数据的成功响应
     *
     * @param message 提示信息
     * @param data 响应数据
     * @param <T> 响应数据类型
     * @return 成功结果
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(true, message, data, HttpStatus.OK.value());
    }

    /**
     * 构造自定义消息的成功响应（无数据）
     *
     * @param message 提示信息
     * @param <T> 响应数据类型
     * @return 成功结果
     */
    public static <T> Result<T> success(String message) {
        return new Result<>(true, message, null, HttpStatus.OK.value());
    }

    /**
     * 构造默认的失败响应（500）
     *
     * @param <T> 响应数据类型
     * @return 失败结果
     */
    public static <T> Result<T> error() {
        return new Result<>(false, "操作失败", null, HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    /**
     * 构造自定义消息的失败响应（500）
     *
     * @param message 错误信息
     * @param <T> 响应数据类型
     * @return 失败结果
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(false, message, null, HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    /**
     * 构造自定义状态码的失败响应
     *
     * @param code 状态码
     * @param message 错误信息
     * @param <T> 响应数据类型
     * @return 失败结果
     */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(false, message, null, code);
    }

    /**
     * 构造基于 HTTP 状态的失败响应
     *
     * @param status HTTP 状态
     * @param message 错误信息
     * @param <T> 响应数据类型
     * @return 失败结果
     */
    public static <T> Result<T> error(HttpStatus status, String message) {
        return new Result<>(false, message, null, status.value());
    }

    /**
     * 构造 400 参数错误响应
     *
     * @param message 错误信息
     * @param <T> 响应数据类型
     * @return 失败结果
     */
    public static <T> Result<T> badRequest(String message) {
        return new Result<>(false, message, null, HttpStatus.BAD_REQUEST.value());
    }

    /**
     * 构造 401 未认证响应
     *
     * @param message 错误信息
     * @param <T> 响应数据类型
     * @return 失败结果
     */
    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(false, message, null, HttpStatus.UNAUTHORIZED.value());
    }

    /**
     * 构造 403 禁止访问响应
     *
     * @param message 错误信息
     * @param <T> 响应数据类型
     * @return 失败结果
     */
    public static <T> Result<T> forbidden(String message) {
        return new Result<>(false, message, null, HttpStatus.FORBIDDEN.value());
    }

    /**
     * 构造 404 资源不存在响应
     *
     * @param message 错误信息
     * @param <T> 响应数据类型
     * @return 失败结果
     */
    public static <T> Result<T> notFound(String message) {
        return new Result<>(false, message, null, HttpStatus.NOT_FOUND.value());
    }
}
