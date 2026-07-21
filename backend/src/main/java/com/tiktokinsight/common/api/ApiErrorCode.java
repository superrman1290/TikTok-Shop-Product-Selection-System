package com.tiktokinsight.common.api;

public enum ApiErrorCode {
    VALIDATION_FAILED(40001, "参数校验失败"),
    JSON_FORMAT_ERROR(40002, "JSON格式错误"),
    UNAUTHENTICATED(40101, "未登录"),
    ACCESS_TOKEN_INVALID(40102, "Access Token无效或过期"),
    REFRESH_TOKEN_INVALID(40103, "Refresh Token无效或过期"),
    INVALID_CREDENTIALS(40104, "用户名或密码错误"),
    ACCOUNT_LOCKED(40105, "账号已锁定"),
    ACCESS_DENIED(40301, "无权访问"),
    ACCOUNT_DISABLED(40302, "账号已禁用"),
    USER_NOT_FOUND(40401, "用户不存在"),
    EMAIL_EXISTS(40901, "邮箱已存在"),
    LOGIN_RATE_LIMITED(42901, "登录尝试过于频繁"),
    INTERNAL_ERROR(50000, "系统内部错误");

    private final int code;
    private final String message;

    ApiErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}
