package com.tiktokinsight.common.api;

import java.time.Instant;

public record ApiResponse<T>(
        int code,
        String message,
        T data,
        String requestId,
        Instant timestamp
) {
    public static <T> ApiResponse<T> success(T data, String requestId) {
        return new ApiResponse<>(0, "success", data, requestId, Instant.now());
    }

    public static <T> ApiResponse<T> failure(
            int code,
            String message,
            T data,
            String requestId
    ) {
        return new ApiResponse<>(code, message, data, requestId, Instant.now());
    }
}
