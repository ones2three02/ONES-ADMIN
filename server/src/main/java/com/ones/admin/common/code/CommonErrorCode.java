package com.ones.admin.common.code;

public enum CommonErrorCode implements ErrorCode {

    SUCCESS(0, "成功"),
    PARAM_ERROR(400, "请求参数不正确"),
    UNAUTHORIZED(401, "登录状态已失效，请重新登录"),
    FORBIDDEN(403, "没有权限访问该资源"),
    NOT_FOUND(404, "资源不存在"),
    BUSINESS_ERROR(4000, "业务处理失败"),
    REPEAT_SUBMIT(4009, "请勿重复提交"),
    FILE_EMPTY(4101, "上传文件不能为空"),
    FILE_EXTENSION_NOT_ALLOWED(4102, "不支持的文件类型"),
    FILE_TOO_LARGE(4103, "上传文件大小超过限制"),
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
