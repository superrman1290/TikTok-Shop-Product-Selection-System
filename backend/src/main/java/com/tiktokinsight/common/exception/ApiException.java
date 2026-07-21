package com.tiktokinsight.common.exception;

import com.tiktokinsight.common.api.ApiErrorCode;
import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final ApiErrorCode errorCode;

    public ApiException(HttpStatus status, ApiErrorCode errorCode) {
        super(errorCode.message());
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus status() {
        return status;
    }

    public ApiErrorCode errorCode() {
        return errorCode;
    }
}
