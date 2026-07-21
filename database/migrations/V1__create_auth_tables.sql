CREATE TABLE user_account (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(254) NOT NULL,
    username VARCHAR(80) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    locked_until TIMESTAMP(6) NULL,
    password_changed_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_account_email UNIQUE (email),
    CONSTRAINT chk_user_account_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT chk_user_account_status CHECK (status IN ('ENABLED', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_refresh_token (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    revoked_at TIMESTAMP(6) NULL,
    replaced_by_token_id BIGINT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    created_by_ip_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES user_account (id),
    CONSTRAINT fk_refresh_token_replacement FOREIGN KEY (replaced_by_token_id) REFERENCES user_refresh_token (id),
    INDEX idx_refresh_token_user_active (user_id, revoked_at, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE login_attempt (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    account_key_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    ip_address_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    successful BOOLEAN NOT NULL,
    attempted_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_login_attempt_user FOREIGN KEY (user_id) REFERENCES user_account (id),
    INDEX idx_login_attempt_account_time (account_key_hash, attempted_at),
    INDEX idx_login_attempt_ip_time (ip_address_hash, attempted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
