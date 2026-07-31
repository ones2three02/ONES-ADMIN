package com.ones.admin.auth;

import com.ones.admin.common.code.ErrorCode;

public enum AuthErrorCode implements ErrorCode {

    INVALID_CREDENTIALS(4201, "用户名或密码错误"),
    ACCOUNT_LOCKED(4202, "账号已被临时锁定，请稍后再试"),
    LOGIN_RATE_LIMITED(4203, "登录尝试过于频繁，请稍后再试"),
    USER_NOT_AVAILABLE(4204, "用户不存在或已停用"),
    OAUTH_PROVIDER_NOT_ENABLED(4210, "第三方登录方式未启用"),
    OAUTH_STATE_INVALID(4211, "OAuth state 无效或已过期"),
    OAUTH_PROVIDER_FAILED(4212, "第三方身份认证失败"),
    EXTERNAL_IDENTITY_NOT_BOUND(4213, "飞书身份尚未绑定 ONES-ADMIN 账号"),
    OAUTH_TICKET_INVALID(4214, "OAuth 登录票据无效或已过期"),
    EXTERNAL_IDENTITY_CONFLICT(4215, "该外部身份已绑定其他账号");

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
