package com.edustudio.common.api;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS("200", "请求成功"),
    BAD_REQUEST("400", "请求参数错误"),
    UNAUTHORIZED("401", "未认证"),
    FORBIDDEN("403", "无访问权限"),
    NOT_FOUND("404", "资源不存在"),
    METHOD_NOT_ALLOWED("405", "请求方法不支持"),
    INTERNAL_ERROR("500", "服务内部错误");

    private final String code;
    private final String message;

    ResultCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
