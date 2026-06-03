package com.edustudio.common.api;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Result<T> {

    private final String code;
    private final String message;
    private final T data;
    private final LocalDateTime timestamp;
    private final boolean success;

    private Result(String code, String message, T data, boolean success) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = success;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data, true);
    }

    public static Result<Void> success() {
        return success(null);
    }

    public static <T> Result<T> failure(ResultCode resultCode) {
        return failure(resultCode, resultCode.getMessage());
    }

    public static <T> Result<T> failure(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null, false);
    }
}
