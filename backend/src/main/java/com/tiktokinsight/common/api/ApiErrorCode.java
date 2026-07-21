package com.tiktokinsight.common.api;

public enum ApiErrorCode {
    VALIDATION_FAILED(40001, "参数校验失败"),
    JSON_FORMAT_ERROR(40002, "JSON格式错误"),
    ACCESS_DENIED(40301, "无权访问"),
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
