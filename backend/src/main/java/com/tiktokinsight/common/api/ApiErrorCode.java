package com.tiktokinsight.common.api;

public enum ApiErrorCode {
    VALIDATION_FAILED(40001, "参数校验失败"),
    JSON_FORMAT_ERROR(40002, "JSON格式错误"),
    CSV_FORMAT_ERROR(40003, "CSV格式错误"),
    UNSUPPORTED_SORT_FIELD(40004, "不支持的排序字段"),
    FILE_TOO_LARGE(40005, "文件超过限制"),
    UNAUTHENTICATED(40101, "未登录"),
    ACCESS_TOKEN_INVALID(40102, "Access Token无效或过期"),
    REFRESH_TOKEN_INVALID(40103, "Refresh Token无效或过期"),
    INVALID_CREDENTIALS(40104, "用户名或密码错误"),
    ACCOUNT_LOCKED(40105, "账号已锁定"),
    ACCESS_DENIED(40301, "无权访问"),
    ACCOUNT_DISABLED(40302, "账号已禁用"),
    USER_NOT_FOUND(40401, "用户不存在"),
    PRODUCT_NOT_FOUND(40402, "商品不存在"),
    IMPORT_JOB_NOT_FOUND(40404, "导入任务不存在"),
    EMAIL_EXISTS(40901, "邮箱已存在"),
    PRODUCT_KEY_CONFLICT(40903, "商品唯一键冲突"),
    DUPLICATE_STAT(40904, "重复统计数据"),
    LOGIN_RATE_LIMITED(42901, "登录尝试过于频繁"),
    INTERNAL_ERROR(50000, "系统内部错误"),
    DATA_SOURCE_UNAVAILABLE(50301, "数据源不可用"),
    DATA_SOURCE_FORMAT_ERROR(50302, "数据源返回格式错误");

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
