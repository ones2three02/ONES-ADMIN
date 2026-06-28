package com.ones.admin.common.web;

import com.ones.admin.common.code.CommonErrorCode;
import com.ones.admin.common.code.ErrorCode;

public record ApiResult<T>(int code, String message, T data) {

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(CommonErrorCode.SUCCESS.code(), CommonErrorCode.SUCCESS.message(), data);
    }

    public static <T> ApiResult<T> fail(int code, String message) {
        return new ApiResult<>(code, message, null);
    }

    public static <T> ApiResult<T> fail(ErrorCode errorCode) {
        return fail(errorCode.code(), errorCode.message());
    }

    public static <T> ApiResult<T> fail(ErrorCode errorCode, String message) {
        return fail(errorCode.code(), message);
    }
}
