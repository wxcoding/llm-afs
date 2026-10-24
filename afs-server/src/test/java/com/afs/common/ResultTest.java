package com.afs.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Result 单元测试
 *
 * 覆盖全部静态工厂方法的 success/message/data/code 字段，
 * 以及时间戳自动填充与 Lombok @Data 生成的访问器。
 */
class ResultTest {

    @Test
    // success() 无参工厂：成功、默认消息、200、无数据
    void successNoArgsReturnsDefaultOkResult() {
        Result<Object> result = Result.success();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("操作成功");
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isNull();
        assertThat(result.getTimestamp()).isGreaterThan(0L);
    }

    @Test
    // success(data) 工厂：数据正确放入泛型字段
    // 注意：success("payload") 这类 String 实参会命中更具体的 success(String message) 重载，
    // 因此这里用非 String 类型验证 success(T data) 的数据承载行为
    void successWithDataCarriesPayload() {
        Result<Integer> result = Result.success(42);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(42);
        assertThat(result.getCode()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    // 重载陷阱固化：String 实参优先落入 success(String message)，data 为 null
    // 调用方若期望 data 携带 String，必须使用 success(message, data) 双参形式
    void stringArgumentResolvesToMessageOverload() {
        Result<String> result = Result.success("payload");

        assertThat(result.getMessage()).isEqualTo("payload");
        assertThat(result.getData()).isNull();
    }

    @Test
    // success(message, data) 工厂：自定义消息与数据
    void successWithMessageAndData() {
        Result<Integer> result = Result.success("自定义", 99);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("自定义");
        assertThat(result.getData()).isEqualTo(99);
    }

    @Test
    // success(message) 工厂：仅自定义消息，数据为 null
    void successWithMessageOnly() {
        Result<Object> result = Result.success("仅消息");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("仅消息");
        assertThat(result.getData()).isNull();
    }

    @Test
    // error() 系列工厂：默认 500、自定义消息、自定义状态码
    void errorFactoriesReturnFailureResults() {
        Result<Object> defaultError = Result.error();
        assertThat(defaultError.isSuccess()).isFalse();
        assertThat(defaultError.getMessage()).isEqualTo("操作失败");
        assertThat(defaultError.getCode()).isEqualTo(500);

        Result<Object> messageError = Result.error("出错了");
        assertThat(messageError.isSuccess()).isFalse();
        assertThat(messageError.getMessage()).isEqualTo("出错了");
        assertThat(messageError.getCode()).isEqualTo(500);

        Result<Object> codedError = Result.error(503, "服务不可用");
        assertThat(codedError.getCode()).isEqualTo(503);
        assertThat(codedError.getMessage()).isEqualTo("服务不可用");

        Result<Object> statusError = Result.error(HttpStatus.BAD_GATEWAY, "网关错误");
        assertThat(statusError.getCode()).isEqualTo(502);
        assertThat(statusError.getMessage()).isEqualTo("网关错误");
    }

    @Test
    // badRequest/unauthorized/forbidden/notFound 工厂的状态码分别为 400/401/403/404
    void semanticErrorFactoriesUseMatchingHttpCodes() {
        assertThat(Result.badRequest("bad").getCode()).isEqualTo(400);
        assertThat(Result.unauthorized("unauth").getCode()).isEqualTo(401);
        assertThat(Result.forbidden("forbid").getCode()).isEqualTo(403);
        assertThat(Result.notFound("nf").getCode()).isEqualTo(404);
        assertThat(Result.notFound("nf").isSuccess()).isFalse();
    }

    @Test
    // 全参构造器与 setter 正常工作（Lombok @Data）
    void constructorAndSettersWork() {
        Result<String> result = new Result<>(true, "msg", "data", 201);
        assertThat(result.getCode()).isEqualTo(201);
        assertThat(result.getTimestamp()).isGreaterThan(0L);

        result.setSuccess(false);
        result.setMessage("changed");
        result.setData("changed-data");
        result.setCode(400);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("changed");
        assertThat(result.getData()).isEqualTo("changed-data");
        assertThat(result.getCode()).isEqualTo(400);
    }
}
