package com.tiktokinsight.datasource;

final class RowValidationException extends RuntimeException {
    private final String fieldName;
    private final String rawValue;
    private final String errorCode;

    RowValidationException(String fieldName, String rawValue, String errorCode, String message) {
        super(message);
        this.fieldName = fieldName;
        this.rawValue = rawValue;
        this.errorCode = errorCode;
    }

    String fieldName() {
        return fieldName;
    }

    String rawValue() {
        return rawValue;
    }

    String errorCode() {
        return errorCode;
    }
}
