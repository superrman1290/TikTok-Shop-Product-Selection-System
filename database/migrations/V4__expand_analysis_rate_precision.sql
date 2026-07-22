ALTER TABLE product_analysis_snapshot
    MODIFY COLUMN sales_growth_rate_7d DECIMAL(18, 4) NULL,
    MODIFY COLUMN sales_growth_rate_30d DECIMAL(18, 4) NULL,
    MODIFY COLUMN video_growth_rate_7d DECIMAL(18, 4) NULL,
    MODIFY COLUMN creator_growth_rate_7d DECIMAL(18, 4) NULL,
    MODIFY COLUMN estimated_profit_margin DECIMAL(18, 4) NULL;
