CREATE TABLE data_source (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    source_type VARCHAR(40) NOT NULL,
    markets_json JSON NOT NULL,
    endpoint_url VARCHAR(500) NULL,
    encrypted_secret TEXT NULL,
    secret_hint VARCHAR(32) NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_data_source_name UNIQUE (name),
    CONSTRAINT fk_data_source_created_by FOREIGN KEY (created_by) REFERENCES user_account (id),
    CONSTRAINT fk_data_source_updated_by FOREIGN KEY (updated_by) REFERENCES user_account (id),
    INDEX idx_data_source_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    operator_user_id BIGINT NULL,
    operator_email VARCHAR(254) NULL,
    action_type VARCHAR(80) NOT NULL,
    target_type VARCHAR(80) NOT NULL,
    target_id VARCHAR(120) NULL,
    request_id VARCHAR(64) NULL,
    request_ip VARCHAR(64) NULL,
    result VARCHAR(20) NOT NULL,
    detail JSON NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_audit_log_operator FOREIGN KEY (operator_user_id) REFERENCES user_account (id) ON DELETE SET NULL,
    INDEX idx_audit_log_operator_created (operator_user_id, created_at),
    INDEX idx_audit_log_target_created (target_type, target_id, created_at),
    INDEX idx_audit_log_action_created (action_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_import_job_completed_status ON import_job (completed_at, status);
CREATE INDEX idx_analysis_job_status_created ON analysis_job (status, created_at);
