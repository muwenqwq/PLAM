package com.learnagent.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一错误码定义
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(200, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有权限访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    AI_SERVICE_UNAVAILABLE(502, "AI 服务不可用"),
    AI_SERVICE_TIMEOUT(504, "AI 服务超时");

    private final int code;
    private final String message;
}
