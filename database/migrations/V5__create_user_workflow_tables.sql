CREATE TABLE watchlist (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_watchlist_user FOREIGN KEY (user_id) REFERENCES user_account (id) ON DELETE CASCADE,
    CONSTRAINT fk_watchlist_product FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE CASCADE,
    CONSTRAINT uk_watchlist_user_product UNIQUE (user_id, product_id),
    INDEX idx_watchlist_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE alert_rule (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    sales_growth_7d_threshold DECIMAL(10, 4) NULL,
    price_drop_7d_threshold DECIMAL(10, 4) NULL,
    competition_score_increase_threshold DECIMAL(10, 4) NULL,
    selection_score_drop_threshold DECIMAL(10, 4) NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_alert_rule_user FOREIGN KEY (user_id) REFERENCES user_account (id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_rule_product FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE CASCADE,
    INDEX idx_alert_rule_user_created (user_id, created_at),
    INDEX idx_alert_rule_enabled_product (enabled, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE alert_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    alert_rule_id BIGINT NOT NULL,
    metric_type VARCHAR(50) NOT NULL,
    stat_date DATE NOT NULL,
    metric_value DECIMAL(18, 4) NOT NULL,
    threshold_value DECIMAL(18, 4) NOT NULL,
    read_status BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_alert_event_user FOREIGN KEY (user_id) REFERENCES user_account (id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_event_product FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE CASCADE,
    CONSTRAINT uk_alert_event_dedup UNIQUE (user_id, product_id, alert_rule_id, metric_type, stat_date),
    INDEX idx_alert_event_user_read_created (user_id, read_status, created_at),
    INDEX idx_alert_event_product_stat (product_id, stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
