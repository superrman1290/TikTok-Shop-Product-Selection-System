package com.tiktokinsight.importing.domain;

import com.tiktokinsight.datasource.ImportRowError;
import java.util.List;

public record BatchWriteResult(int successfulRows, List<ImportRowError> errors) {
    public static BatchWriteResult success(int rows) {
        return new BatchWriteResult(rows, List.of());
    }
}
