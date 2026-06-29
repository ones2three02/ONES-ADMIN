package com.ones.admin.common.code;

public enum CommonErrorCode implements ErrorCode {

    SUCCESS(0, "成功"),
    PARAM_ERROR(400, "请求参数不正确"),
    UNAUTHORIZED(401, "登录状态已失效，请重新登录"),
    FORBIDDEN(403, "没有权限访问该资源"),
    NOT_FOUND(404, "资源不存在"),
    BUSINESS_ERROR(4000, "业务处理失败"),
    REPEAT_SUBMIT(4009, "请勿重复提交"),
    SYSTEM_ERROR(500, "系统异常，请稍后重试");

    private final int code;
    private final String message;

    CommonErrorCode(int code, String message) {
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
