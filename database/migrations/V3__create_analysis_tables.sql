CREATE TABLE market_cost_profile (
    id BIGINT NOT NULL AUTO_INCREMENT,
    market VARCHAR(10) NOT NULL,
    currency CHAR(3) NOT NULL,
    purchase_cost DECIMAL(18, 2) NOT NULL,
    domestic_shipping_cost DECIMAL(18, 2) NOT NULL,
    international_shipping_cost DECIMAL(18, 2) NOT NULL,
    platform_commission_rate DECIMAL(10, 4) NOT NULL,
    payment_fee_rate DECIMAL(10, 4) NOT NULL,
    advertising_cost_rate DECIMAL(10, 4) NOT NULL,
    refund_loss_rate DECIMAL(10, 4) NOT NULL,
    other_cost DECIMAL(18, 2) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_market_cost_profile_market UNIQUE (market)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at TIMESTAMP(3) NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_product_cost_profile (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    purchase_cost DECIMAL(18, 2) NOT NULL,
    domestic_shipping_cost DECIMAL(18, 2) NOT NULL,
    international_shipping_cost DECIMAL(18, 2) NOT NULL,
    platform_commission_rate DECIMAL(10, 4) NOT NULL,
    payment_fee_rate DECIMAL(10, 4) NOT NULL,
    advertising_cost_rate DECIMAL(10, 4) NOT NULL,
    refund_loss_rate DECIMAL(10, 4) NOT NULL,
    other_cost DECIMAL(18, 2) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_cost_profile_user FOREIGN KEY (user_id) REFERENCES user_account (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_cost_profile_product FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE CASCADE,
    CONSTRAINT uk_user_product_cost_profile UNIQUE (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE category_benchmark_daily (
    id BIGINT NOT NULL AUTO_INCREMENT,
    market VARCHAR(10) NOT NULL,
    category_id BIGINT NOT NULL,
    stat_date DATE NOT NULL,
    algorithm_version VARCHAR(80) NOT NULL,
    sample_count BIGINT NOT NULL,
    sales_volume_7d_p75 BIGINT NULL,
    calculated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_category_benchmark_category FOREIGN KEY (category_id) REFERENCES category (id),
    CONSTRAINT uk_category_benchmark UNIQUE (market, category_id, stat_date, algorithm_version),
    INDEX idx_category_benchmark_lookup (market, category_id, stat_date, algorithm_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE analysis_job (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    analysis_date DATE NOT NULL,
    algorithm_version VARCHAR(80) NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP(6) NOT NULL,
    started_at TIMESTAMP(6) NULL,
    completed_at TIMESTAMP(6) NULL,
    error_message VARCHAR(500) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_analysis_job_product FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE CASCADE,
    CONSTRAINT uk_analysis_job UNIQUE (product_id, analysis_date, algorithm_version),
    CONSTRAINT chk_analysis_job_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED', 'CANCELLED')),
    INDEX idx_analysis_job_status_retry (status, next_retry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE product_analysis_snapshot (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    market VARCHAR(10) NOT NULL,
    category_id BIGINT NOT NULL,
    analysis_date DATE NOT NULL,
    algorithm_version VARCHAR(80) NOT NULL,
    score_status VARCHAR(30) NOT NULL,
    sales_volume_7d BIGINT NULL,
    sales_growth_rate_7d DECIMAL(10, 4) NULL,
    sales_growth_rate_30d DECIMAL(10, 4) NULL,
    video_growth_rate_7d DECIMAL(10, 4) NULL,
    creator_growth_rate_7d DECIMAL(10, 4) NULL,
    trend_score DECIMAL(10, 2) NULL,
    competition_score DECIMAL(10, 2) NULL,
    estimated_profit DECIMAL(18, 2) NULL,
    estimated_profit_margin DECIMAL(10, 4) NULL,
    profit_score DECIMAL(10, 2) NULL,
    risk_score DECIMAL(10, 2) NULL,
    selection_score DECIMAL(10, 2) NULL,
    lifecycle_stage VARCHAR(20) NULL,
    recommendation VARCHAR(30) NOT NULL,
    score_details_json JSON NOT NULL,
    reasons_json JSON NOT NULL,
    risks_json JSON NOT NULL,
    source_data_updated_at TIMESTAMP(6) NULL,
    calculated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_analysis_snapshot_product FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE CASCADE,
    CONSTRAINT fk_analysis_snapshot_category FOREIGN KEY (category_id) REFERENCES category (id),
    CONSTRAINT uk_analysis_snapshot UNIQUE (product_id, analysis_date, algorithm_version),
    INDEX idx_analysis_snapshot_product_date_version (product_id, analysis_date, algorithm_version),
    INDEX idx_analysis_snapshot_market_category_score (market, category_id, analysis_date, algorithm_version, selection_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO market_cost_profile (
    market, currency, purchase_cost, domestic_shipping_cost, international_shipping_cost,
    platform_commission_rate, payment_fee_rate, advertising_cost_rate, refund_loss_rate, other_cost, created_at, updated_at
) VALUES
    ('US', 'USD', 6.50, 0.80, 5.20, 6.0000, 2.9000, 12.0000, 3.0000, 1.00, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('GB', 'GBP', 5.20, 0.64, 4.16, 6.0000, 2.9000, 12.0000, 3.0000, 0.80, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('TH', 'THB', 230.00, 28.00, 185.00, 6.0000, 2.9000, 12.0000, 3.0000, 35.00, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('VN', 'VND', 160000.00, 20000.00, 128000.00, 6.0000, 2.9000, 12.0000, 3.0000, 24000.00, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('PH', 'PHP', 360.00, 44.00, 288.00, 6.0000, 2.9000, 12.0000, 3.0000, 55.00, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('MY', 'MYR', 30.00, 3.60, 24.00, 6.0000, 2.9000, 12.0000, 3.0000, 4.50, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('SG', 'SGD', 9.00, 1.10, 7.20, 6.0000, 2.9000, 12.0000, 3.0000, 1.40, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('ID', 'IDR', 100000.00, 12000.00, 80000.00, 6.0000, 2.9000, 12.0000, 3.0000, 15000.00, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));
