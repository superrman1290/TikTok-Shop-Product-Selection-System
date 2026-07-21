package com.tiktokinsight.importing.domain;

import com.tiktokinsight.datasource.DataSourceType;
import java.time.Instant;

public record ImportJob(
        long id,
        ImportType importType,
        DataSourceType sourceType,
        ImportStatus status,
        String originalFileName,
        long totalRows,
        long successRows,
        long failedRows,
        long createdBy,
        String createdByUsername,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt
) {
}
