package com.tiktokinsight.datasource;

public record ImportRow<T>(long rowNumber, T data, ImportRowError error) {
    public static <T> ImportRow<T> success(long rowNumber, T data) {
        return new ImportRow<>(rowNumber, data, null);
    }

    public static <T> ImportRow<T> failure(ImportRowError error) {
        return new ImportRow<>(error.rowNumber(), null, error);
    }

    public boolean valid() {
        return error == null;
    }
}
