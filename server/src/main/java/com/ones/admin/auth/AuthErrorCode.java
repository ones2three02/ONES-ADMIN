package com.ones.admin.auth;

import com.ones.admin.common.code.ErrorCode;

public enum AuthErrorCode implements ErrorCode {

    INVALID_CREDENTIALS(4201, "用户名或密码错误"),
    ACCOUNT_LOCKED(4202, "账号已被临时锁定，请稍后再试"),
    USER_NOT_AVAILABLE(4204, "用户不存在或已停用");

    private final int code;
    private final String message;

    AuthErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
