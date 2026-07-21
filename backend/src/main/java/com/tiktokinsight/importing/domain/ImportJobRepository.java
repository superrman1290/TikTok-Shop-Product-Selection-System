package com.tiktokinsight.importing.domain;

import com.tiktokinsight.common.api.PageResponse;
import com.tiktokinsight.datasource.DataSourceType;
import com.tiktokinsight.datasource.ImportRowError;
import java.util.List;
import java.util.Optional;

public interface ImportJobRepository {
    long create(ImportType type, DataSourceType sourceType, String fileName, String objectKey, long userId);

    void increment(long jobId, long totalRows, long successRows, long failedRows);

    void saveErrors(long jobId, List<ImportRowError> errors);

    void complete(long jobId, ImportStatus status);

    void fail(long jobId);

    Optional<ImportJob> findById(long jobId);

    PageResponse<ImportJob> findAll(int page, int pageSize);

    PageResponse<ImportRowError> findErrors(long jobId, int page, int pageSize);
}
