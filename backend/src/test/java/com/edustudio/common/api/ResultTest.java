package com.edustudio.common.api;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ResultTest {

    @Test
    void successWrapsDataWithUnifiedFields() {
        Result<Map<String, String>> result = Result.success(Map.of("status", "UP"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCode()).isEqualTo(ResultCode.SUCCESS.getCode());
        assertThat(result.getMessage()).isEqualTo(ResultCode.SUCCESS.getMessage());
        assertThat(result.getData()).containsEntry("status", "UP");
        assertThat(result.getTimestamp()).isNotNull();
    }

    @Test
    void failureWrapsCodeAndMessageWithoutStackTrace() {
        Result<Void> result = Result.failure(ResultCode.INTERNAL_ERROR, "服务内部错误");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getCode()).isEqualTo(ResultCode.INTERNAL_ERROR.getCode());
        assertThat(result.getMessage()).isEqualTo("服务内部错误");
        assertThat(result.getData()).isNull();
        assertThat(result.getTimestamp()).isNotNull();
    }
}
