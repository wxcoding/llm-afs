package com.afs.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BusinessException 单元测试
 *
 * 覆盖全部构造器（默认 400、携带 cause、指定状态码）、
 * 静态工厂方法与 status 非空校验。
 */
class BusinessExceptionTest {

    @Test
    // 仅传消息时默认状态码为 400
    void messageOnlyConstructorDefaultsToBadRequest() {
        BusinessException e = new BusinessException("业务失败");

        assertThat(e.getMessage()).isEqualTo("业务失败");
        assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    // 消息 + cause 构造器：默认 400 且保留原因链
    void messageAndCauseConstructorKeepsCause() {
        RuntimeException cause = new RuntimeException("root");
        BusinessException e = new BusinessException("业务失败", cause);

        assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(e.getCause()).isSameAs(cause);
    }

    @Test
    // 可指定任意 HttpStatus
    void statusConstructorUsesGivenStatus() {
        BusinessException e = new BusinessException(HttpStatus.CONFLICT, "冲突");

        assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(e.getMessage()).isEqualTo("冲突");
    }

    @Test
    // status + cause 构造器同时保留两者
    void statusAndCauseConstructorKeepsBoth() {
        RuntimeException cause = new RuntimeException("root");
        BusinessException e = new BusinessException(HttpStatus.FORBIDDEN, "禁止", cause);

        assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(e.getCause()).isSameAs(cause);
    }

    @Test
    // 静态工厂方法与对应状态码匹配
    void staticFactoriesUseMatchingStatuses() {
        assertThat(BusinessException.notFound("m").getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(BusinessException.badRequest("m").getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(BusinessException.unauthorized("m").getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(BusinessException.forbidden("m").getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(BusinessException.serverError("m").getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(BusinessException.notFound("m").getMessage()).isEqualTo("m");
    }

    @Test
    // status 为 null 时抛出 NullPointerException
    void nullStatusIsRejected() {
        assertThatThrownBy(() -> new BusinessException(null, "msg"))
                .isInstanceOf(NullPointerException.class);
    }
}
