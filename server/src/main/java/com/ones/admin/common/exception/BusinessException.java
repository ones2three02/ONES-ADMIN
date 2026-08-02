package com.ones.admin.common.exception;

import com.ones.admin.common.code.CommonErrorCode;
import com.ones.admin.common.code.ErrorCode;

public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(CommonErrorCode.BUSINESS_ERROR.code(), message);
    }

    public BusinessException(ErrorCode errorCode) {
        this(errorCode.code(), errorCode.message());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode.code(), message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
