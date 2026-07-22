package com.tiktokinsight.importing.infrastructure;

import com.tiktokinsight.common.api.PageResponse;
import com.tiktokinsight.datasource.DataSourceType;
import com.tiktokinsight.datasource.ImportRowError;
import com.tiktokinsight.importing.domain.ImportJob;
import com.tiktokinsight.importing.domain.ImportJobRepository;
import com.tiktokinsight.importing.domain.ImportStatus;
import com.tiktokinsight.importing.domain.ImportType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcImportJobRepository implements ImportJobRepository {

    private static final String SELECT = """
            SELECT j.id, j.import_type, j.source_type, j.status, j.original_file_name,
                   j.total_rows, j.success_rows, j.failed_rows, j.created_by,
                   u.username AS created_by_username, j.started_at, j.completed_at, j.created_at
              FROM import_job j
              JOIN user_account u ON u.id = j.created_by
            """;

    private final NamedParameterJdbcTemplate namedJdbc;
    private final JdbcTemplate jdbc;

    public JdbcImportJobRepository(NamedParameterJdbcTemplate namedJdbc, JdbcTemplate jdbc) {
        this.namedJdbc = namedJdbc;
        this.jdbc = jdbc;
    }

    @Override
    public long create(ImportType type, DataSourceType sourceType, String fileName, String objectKey, long userId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        namedJdbc.update("""
                INSERT INTO import_job(
                    import_type, source_type, status, original_file_name, object_key,
                    created_by, started_at, created_at, updated_at
                ) VALUES (
                    :type, :sourceType, 'RUNNING', :fileName, :objectKey,
                    :userId, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
                )
                """, new MapSqlParameterSource()
                .addValue("type", type.name())
                .addValue("sourceType", sourceType.name())
                .addValue("fileName", fileName)
                .addValue("objectKey", objectKey)
                .addValue("userId", userId), keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Import job id was not generated");
        }
        return key.longValue();
    }

    @Override
    public void increment(long jobId, long totalRows, long successRows, long failedRows) {
        namedJdbc.update("""
                UPDATE import_job
                   SET total_rows = total_rows + :totalRows,
                       success_rows = success_rows + :successRows,
                       failed_rows = failed_rows + :failedRows,
                       updated_at = UTC_TIMESTAMP(6)
                 WHERE id = :id
                """, Map.of(
                "id", jobId,
                "totalRows", totalRows,
                "successRows", successRows,
                "failedRows", failedRows
        ));
    }

    @Override
    public void saveErrors(long jobId, List<ImportRowError> errors) {
        if (errors.isEmpty()) {
            return;
        }
        jdbc.batchUpdate("""
                INSERT INTO import_job_error(
                    import_job_id, row_index, field_name, raw_value, error_code, error_message, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(6))
                """, errors, 1000, (statement, error) -> {
            statement.setLong(1, jobId);
            statement.setLong(2, error.rowNumber());
            statement.setString(3, error.fieldName());
            statement.setString(4, error.rawValue());
            statement.setString(5, error.errorCode());
            statement.setString(6, error.errorMessage());
        });
    }

    @Override
    public void complete(long jobId, ImportStatus status) {
        namedJdbc.update("""
                UPDATE import_job
                   SET status = :status, completed_at = UTC_TIMESTAMP(6), updated_at = UTC_TIMESTAMP(6)
                 WHERE id = :id
                """, Map.of("id", jobId, "status", status.name()));
    }

    @Override
    public void fail(long jobId) {
        complete(jobId, ImportStatus.FAILED);
    }

    @Override
    public Optional<ImportJob> findById(long jobId) {
        List<ImportJob> jobs = namedJdbc.query(SELECT + " WHERE j.id = :id", Map.of("id", jobId), this::mapJob);
        return jobs.stream().findFirst();
    }

    @Override
    public PageResponse<ImportJob> findAll(int page, int pageSize) {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM import_job", Long.class);
        List<ImportJob> jobs = namedJdbc.query(
                SELECT + " ORDER BY j.created_at DESC, j.id DESC LIMIT :limit OFFSET :offset",
                Map.of("limit", pageSize, "offset", (page - 1) * pageSize),
                this::mapJob
        );
        return new PageResponse<>(page, pageSize, total == null ? 0 : total, jobs);
    }

    @Override
    public PageResponse<ImportRowError> findErrors(long jobId, int page, int pageSize) {
        Long total = namedJdbc.queryForObject(
                "SELECT COUNT(*) FROM import_job_error WHERE import_job_id = :jobId",
                Map.of("jobId", jobId), Long.class
        );
        List<ImportRowError> errors = namedJdbc.query("""
                SELECT row_index, field_name, raw_value, error_code, error_message
                  FROM import_job_error
                 WHERE import_job_id = :jobId
                 ORDER BY row_index ASC, id ASC
                 LIMIT :limit OFFSET :offset
                """, Map.of(
                "jobId", jobId,
                "limit", pageSize,
                "offset", (page - 1) * pageSize
        ), (resultSet, rowNumber) -> new ImportRowError(
                resultSet.getLong("row_index"),
                resultSet.getString("field_name"),
                resultSet.getString("raw_value"),
                resultSet.getString("error_code"),
                resultSet.getString("error_message")
        ));
        return new PageResponse<>(page, pageSize, total == null ? 0 : total, errors);
    }

    private ImportJob mapJob(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ImportJob(
                resultSet.getLong("id"),
                ImportType.valueOf(resultSet.getString("import_type")),
                DataSourceType.valueOf(resultSet.getString("source_type")),
                ImportStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("original_file_name"),
                resultSet.getLong("total_rows"),
                resultSet.getLong("success_rows"),
                resultSet.getLong("failed_rows"),
                resultSet.getLong("created_by"),
                resultSet.getString("created_by_username"),
                instant(resultSet.getTimestamp("started_at")),
                instant(resultSet.getTimestamp("completed_at")),
                instant(resultSet.getTimestamp("created_at"))
        );
    }

    private java.time.Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
