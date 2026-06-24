package com.edustudio.common.exception;

import com.edustudio.common.api.ResultCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;

    public BusinessException(String message) {
        this(ResultCode.BAD_REQUEST, message);
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }
}
