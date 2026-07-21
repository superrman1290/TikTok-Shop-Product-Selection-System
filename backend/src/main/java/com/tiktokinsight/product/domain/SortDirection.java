package com.tiktokinsight.product.domain;

import com.tiktokinsight.common.api.ApiErrorCode;
import com.tiktokinsight.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public enum SortDirection {
    ASC,
    DESC;

    public static SortDirection fromApiValue(String value) {
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED);
        }
    }
}
