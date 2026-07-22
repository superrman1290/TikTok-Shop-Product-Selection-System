package com.tiktokinsight.datasource;

public record ImportRowError(
        long rowNumber,
        String fieldName,
        String rawValue,
        String errorCode,
        String errorMessage
) {
}
