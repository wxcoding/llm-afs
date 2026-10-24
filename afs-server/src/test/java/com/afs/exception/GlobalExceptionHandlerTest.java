package com.afs.exception;

import com.afs.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GlobalExceptionHandler 单元测试
 *
 * 直接调用各 @ExceptionHandler 方法，断言返回的 Result / ResponseEntity
 * 状态码、消息与错误明细。
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        // 打桩请求 URI，满足日志输出
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    // 业务异常：ResponseEntity 状态码与 Result 内容均取自异常
    void handleBusinessExceptionReturnsStatusAndMessage() {
        BusinessException e = new BusinessException(HttpStatus.NOT_FOUND, "资源不存在");

        ResponseEntity<Result<Void>> entity = handler.handleBusinessException(e, request);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().isSuccess()).isFalse();
        assertThat(entity.getBody().getCode()).isEqualTo(404);
        assertThat(entity.getBody().getMessage()).isEqualTo("资源不存在");
    }

    @Test
    // @Valid 校验失败：返回字段名到错误消息的映射
    void handleValidExceptionCollectsFieldErrors() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "name", "名称不能为空"));
        bindingResult.addError(new FieldError("target", "age", "年龄必须为正数"));
        MethodArgumentNotValidException e = mock(MethodArgumentNotValidException.class);
        // 打桩：绑定结果包含两个字段错误
        when(e.getBindingResult()).thenReturn(bindingResult);

        Result<Map<String, String>> result = handler.handleValidException(e);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).isEqualTo("参数校验失败");
        assertThat(result.getData()).containsEntry("name", "名称不能为空")
                .containsEntry("age", "年龄必须为正数");
    }

    @Test
    // 绑定异常：同样聚合字段错误
    void handleBindExceptionCollectsFieldErrors() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "phone", "手机号格式错误"));

        Result<Map<String, String>> result =
                handler.handleBindException(new BindException(bindingResult));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("参数绑定失败");
        assertThat(result.getData()).containsOnly(Map.entry("phone", "手机号格式错误"));
    }

    @Test
    // 缺少请求参数：消息中包含参数名
    void handleMissingParameterReturnsBadRequest() {
        MissingServletRequestParameterException e =
                new MissingServletRequestParameterException("page", "int");

        Result<Void> result = handler.handleMissingServletRequestParameterException(e, request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).contains("page");
    }

    @Test
    // 请求方法不支持：返回 405 与方法名
    void handleMethodNotSupportedReturns405() {
        HttpRequestMethodNotSupportedException e = new HttpRequestMethodNotSupportedException("DELETE");

        Result<Void> result = handler.handleHttpRequestMethodNotSupportedException(e, request);

        assertThat(result.getCode()).isEqualTo(405);
        assertThat(result.getMessage()).contains("DELETE");
    }

    @Test
    // 无匹配处理器：返回 404 与请求路径
    void handleNoHandlerFoundReturns404() {
        NoHandlerFoundException e = new NoHandlerFoundException("GET", "/api/missing", new HttpHeaders());

        Result<Void> result = handler.handleNoHandlerFoundException(e, request);

        assertThat(result.getCode()).isEqualTo(404);
        assertThat(result.getMessage()).contains("/api/missing");
    }

    @Test
    // 上传超限：返回 400 与固定提示
    void handleMaxUploadSizeExceededReturnsBadRequest() {
        MaxUploadSizeExceededException e = new MaxUploadSizeExceededException(10_485_760L);

        Result<Void> result = handler.handleMaxUploadSizeExceededException(e, request);

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).contains("10MB");
    }

    @Test
    // 空指针异常：返回 500 与兜底提示，不泄露内部细节
    void handleNullPointerReturns500() {
        Result<Void> result = handler.handleNullPointerException(new NullPointerException("boom"), request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).isEqualTo("系统内部错误，请稍后重试");
    }

    @Test
    // 未知异常：返回 500 与兜底提示
    void handleGenericExceptionReturns500() {
        Result<Void> result = handler.handleException(new RuntimeException("unexpected"), request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).isEqualTo("系统内部错误，请稍后重试");
    }
}
