package com.afs.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ResultCode 单元测试
 *
 * 覆盖枚举常量的 code/message 取值与 valueOf 解析。
 */
class ResultCodeTest {

    @Test
    // 各枚举常量的状态码与消息符合约定
    void enumConstantsCarryExpectedCodeAndMessage() {
        assertThat(ResultCode.SUCCESS.getCode()).isEqualTo(200);
        assertThat(ResultCode.SUCCESS.getMessage()).isEqualTo("操作成功");
        assertThat(ResultCode.BAD_REQUEST.getCode()).isEqualTo(400);
        assertThat(ResultCode.BAD_REQUEST.getMessage()).isEqualTo("参数错误");
        assertThat(ResultCode.UNAUTHORIZED.getCode()).isEqualTo(401);
        assertThat(ResultCode.UNAUTHORIZED.getMessage()).isEqualTo("未认证");
        assertThat(ResultCode.FORBIDDEN.getCode()).isEqualTo(403);
        assertThat(ResultCode.FORBIDDEN.getMessage()).isEqualTo("禁止访问");
        assertThat(ResultCode.NOT_FOUND.getCode()).isEqualTo(404);
        assertThat(ResultCode.NOT_FOUND.getMessage()).isEqualTo("资源不存在");
        assertThat(ResultCode.INTERNAL_ERROR.getCode()).isEqualTo(500);
        assertThat(ResultCode.INTERNAL_ERROR.getMessage()).isEqualTo("系统异常");
    }

    @Test
    // 枚举数量固定为 6 个，valueOf 可按名解析
    void valueOfResolvesByName() {
        assertThat(ResultCode.values()).hasSize(6);
        assertThat(ResultCode.valueOf("SUCCESS")).isSameAs(ResultCode.SUCCESS);
        assertThat(ResultCode.valueOf("INTERNAL_ERROR")).isSameAs(ResultCode.INTERNAL_ERROR);
    }
}
