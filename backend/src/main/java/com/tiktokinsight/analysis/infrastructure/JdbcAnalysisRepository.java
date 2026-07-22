package com.tiktokinsight.analysis.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiktokinsight.analysis.domain.AnalysisDailyStat;
import com.tiktokinsight.analysis.domain.AnalysisJob;
import com.tiktokinsight.analysis.domain.AnalysisProduct;
import com.tiktokinsight.analysis.domain.AnalysisRepository;
import com.tiktokinsight.analysis.domain.AnalysisResult;
import com.tiktokinsight.analysis.domain.AnalysisTarget;
import com.tiktokinsight.analysis.domain.CategoryBenchmark;
import com.tiktokinsight.analysis.domain.CostProfile;
import com.tiktokinsight.analysis.domain.LifecycleStage;
import com.tiktokinsight.analysis.domain.ProfitCalculation;
import com.tiktokinsight.analysis.domain.Recommendation;
import com.tiktokinsight.analysis.domain.ScoreDetail;
import com.tiktokinsight.analysis.domain.ScoreStatus;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAnalysisRepository implements AnalysisRepository {

    private static final TypeReference<List<ScoreDetail>> SCORE_DETAILS = new TypeReference<>() { };
    private static final TypeReference<List<String>> STRINGS = new TypeReference<>() { };
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public JdbcAnalysisRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<AnalysisProduct> findProduct(long productId) {
        List<AnalysisProduct> products = jdbc.query("""
                SELECT id, market, category_id, currency, title,
                       DATE(listed_at) AS listed_date, latest_stat_date
                  FROM product
                 WHERE id = :productId AND status = 'ACTIVE'
                """, Map.of("productId", productId), (resultSet, rowNumber) -> new AnalysisProduct(
                resultSet.getLong("id"), resultSet.getString("market"), resultSet.getLong("category_id"),
                resultSet.getString("currency"), resultSet.getString("title"),
                resultSet.getObject("listed_date", LocalDate.class), resultSet.getObject("latest_stat_date", LocalDate.class)
        ));
        return products.stream().findFirst();
    }

    @Override
    public List<AnalysisDailyStat> findStats(long productId, LocalDate endDate) {
        return jdbc.query("""
                SELECT stat_date, price, sales_volume, video_count, creator_count, shop_count, similar_product_count,
                       top10_shop_sales_share, top10_creator_sales_share, negative_review_rate, rating
                  FROM product_daily_stat
                 WHERE product_id = :productId AND stat_date <= :endDate
                 ORDER BY stat_date ASC
                 LIMIT 366
                """, Map.of("productId", productId, "endDate", endDate), (resultSet, rowNumber) -> stat(resultSet));
    }

    @Override
    public Optional<CostProfile> findMarketCost(String market) {
        return findCost("SELECT * FROM market_cost_profile WHERE market = :market", Map.of("market", market));
    }

    @Override
    public Optional<CostProfile> findUserCost(long userId, long productId) {
        return findCost("SELECT * FROM user_product_cost_profile WHERE user_id = :userId AND product_id = :productId",
                Map.of("userId", userId, "productId", productId));
    }

    @Override
    public void saveUserCost(long userId, long productId, CostProfile profile, Instant now) {
        jdbc.update("""
                INSERT INTO user_product_cost_profile (
                    user_id, product_id, currency, purchase_cost, domestic_shipping_cost, international_shipping_cost,
                    platform_commission_rate, payment_fee_rate, advertising_cost_rate, refund_loss_rate, other_cost, created_at, updated_at
                ) VALUES (
                    :userId, :productId, :currency, :purchaseCost, :domesticShippingCost, :internationalShippingCost,
                    :platformCommissionRate, :paymentFeeRate, :advertisingCostRate, :refundLossRate, :otherCost, :now, :now
                ) ON DUPLICATE KEY UPDATE
                    currency = VALUES(currency), purchase_cost = VALUES(purchase_cost),
                    domestic_shipping_cost = VALUES(domestic_shipping_cost), international_shipping_cost = VALUES(international_shipping_cost),
                    platform_commission_rate = VALUES(platform_commission_rate), payment_fee_rate = VALUES(payment_fee_rate),
                    advertising_cost_rate = VALUES(advertising_cost_rate), refund_loss_rate = VALUES(refund_loss_rate),
                    other_cost = VALUES(other_cost), updated_at = VALUES(updated_at)
                """, costParameters(userId, productId, profile, now));
    }

    @Override
    public boolean deleteUserCost(long userId, long productId) {
        return jdbc.update("DELETE FROM user_product_cost_profile WHERE user_id = :userId AND product_id = :productId",
                Map.of("userId", userId, "productId", productId)) > 0;
    }

    @Override
    public Optional<CategoryBenchmark> findBenchmark(String market, long categoryId, LocalDate statDate, String algorithmVersion) {
        List<CategoryBenchmark> results = jdbc.query("""
                SELECT sample_count, sales_volume_7d_p75
                  FROM category_benchmark_daily
                 WHERE market = :market AND category_id = :categoryId AND stat_date = :statDate
                   AND algorithm_version = :algorithmVersion
                """, Map.of("market", market, "categoryId", categoryId, "statDate", statDate, "algorithmVersion", algorithmVersion),
                (resultSet, rowNumber) -> new CategoryBenchmark(resultSet.getLong("sample_count"), nullableLong(resultSet, "sales_volume_7d_p75")));
        return results.stream().findFirst();
    }

    @Override
    public void saveBenchmark(String market, long categoryId, LocalDate statDate, String algorithmVersion,
                              CategoryBenchmark benchmark, Instant now) {
        jdbc.update("""
                INSERT INTO category_benchmark_daily (
                    market, category_id, stat_date, algorithm_version, sample_count, sales_volume_7d_p75, calculated_at
                ) VALUES (:market, :categoryId, :statDate, :algorithmVersion, :sampleCount, :p75, :now)
                ON DUPLICATE KEY UPDATE sample_count = VALUES(sample_count), sales_volume_7d_p75 = VALUES(sales_volume_7d_p75),
                    calculated_at = VALUES(calculated_at)
                """, new MapSqlParameterSource().addValue("market", market).addValue("categoryId", categoryId)
                .addValue("statDate", statDate).addValue("algorithmVersion", algorithmVersion).addValue("sampleCount", benchmark.sampleCount())
                .addValue("p75", benchmark.salesVolume7dP75()).addValue("now", now));
    }

    @Override
    public List<Long> findActiveProductIds(String market, long categoryId) {
        return jdbc.queryForList("""
                SELECT id FROM product WHERE market = :market AND category_id = :categoryId AND status = 'ACTIVE'
                """, Map.of("market", market, "categoryId", categoryId), Long.class);
    }

    @Override
    public List<Long> findSevenDaySalesVolumes(String market, long categoryId, LocalDate statDate) {
        return jdbc.queryForList("""
                SELECT SUM(stat.sales_volume) AS sales_volume_7d
                  FROM product product
                  JOIN product_daily_stat stat ON stat.product_id = product.id
                 WHERE product.market = :market AND product.category_id = :categoryId AND product.status = 'ACTIVE'
                   AND stat.stat_date BETWEEN :startDate AND :statDate
                 GROUP BY product.id
                HAVING COUNT(*) = 7
                 ORDER BY sales_volume_7d ASC
                """, Map.of("market", market, "categoryId", categoryId, "startDate", statDate.minusDays(6), "statDate", statDate), Long.class);
    }

    @Override
    public Optional<AnalysisResult> findSnapshot(long productId, LocalDate analysisDate, String algorithmVersion) {
        return findSnapshot("""
                SELECT * FROM product_analysis_snapshot
                 WHERE product_id = :productId AND analysis_date = :analysisDate AND algorithm_version = :algorithmVersion
                """, Map.of("productId", productId, "analysisDate", analysisDate, "algorithmVersion", algorithmVersion));
    }

    @Override
    public Optional<AnalysisResult> findLatestSnapshot(long productId, String algorithmVersion) {
        return findSnapshot("""
                SELECT * FROM product_analysis_snapshot
                 WHERE product_id = :productId AND algorithm_version = :algorithmVersion
                 ORDER BY analysis_date DESC LIMIT 1
                """, Map.of("productId", productId, "algorithmVersion", algorithmVersion));
    }

    @Override
    public void saveSnapshot(AnalysisResult result, Instant sourceDataUpdatedAt, Instant calculatedAt) {
        ProfitCalculation profit = result.profitCalculation();
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("productId", result.productId()).addValue("market", result.market()).addValue("categoryId", result.categoryId())
                .addValue("analysisDate", result.analysisDate()).addValue("algorithmVersion", result.algorithmVersion())
                .addValue("scoreStatus", result.scoreStatus().name()).addValue("salesVolume7d", result.salesVolume7d())
                .addValue("salesGrowth7d", result.salesGrowthRate7d()).addValue("salesGrowth30d", result.salesGrowthRate30d())
                .addValue("videoGrowth7d", result.videoGrowthRate7d()).addValue("creatorGrowth7d", result.creatorGrowthRate7d())
                .addValue("trendScore", result.trendScore()).addValue("competitionScore", result.competitionScore())
                .addValue("estimatedProfit", profit == null ? null : profit.estimatedProfit())
                .addValue("estimatedProfitMargin", profit == null ? null : profit.estimatedProfitMargin())
                .addValue("profitScore", result.profitScore()).addValue("riskScore", result.riskScore()).addValue("selectionScore", result.selectionScore())
                .addValue("lifecycleStage", result.lifecycleStage() == null ? null : result.lifecycleStage().name())
                .addValue("recommendation", result.recommendation().name()).addValue("details", write(result.scoreDetails()))
                .addValue("reasons", write(result.reasons())).addValue("risks", write(result.risks()))
                .addValue("sourceDataUpdatedAt", sourceDataUpdatedAt).addValue("calculatedAt", calculatedAt);
        jdbc.update("""
                INSERT INTO product_analysis_snapshot (
                    product_id, market, category_id, analysis_date, algorithm_version, score_status, sales_volume_7d,
                    sales_growth_rate_7d, sales_growth_rate_30d, video_growth_rate_7d, creator_growth_rate_7d,
                    trend_score, competition_score, estimated_profit, estimated_profit_margin, profit_score, risk_score,
                    selection_score, lifecycle_stage, recommendation, score_details_json, reasons_json, risks_json,
                    source_data_updated_at, calculated_at
                ) VALUES (
                    :productId, :market, :categoryId, :analysisDate, :algorithmVersion, :scoreStatus, :salesVolume7d,
                    :salesGrowth7d, :salesGrowth30d, :videoGrowth7d, :creatorGrowth7d, :trendScore, :competitionScore,
                    :estimatedProfit, :estimatedProfitMargin, :profitScore, :riskScore, :selectionScore, :lifecycleStage,
                    :recommendation, :details, :reasons, :risks, :sourceDataUpdatedAt, :calculatedAt
                ) ON DUPLICATE KEY UPDATE
                    score_status = VALUES(score_status), sales_volume_7d = VALUES(sales_volume_7d),
                    sales_growth_rate_7d = VALUES(sales_growth_rate_7d), sales_growth_rate_30d = VALUES(sales_growth_rate_30d),
                    video_growth_rate_7d = VALUES(video_growth_rate_7d), creator_growth_rate_7d = VALUES(creator_growth_rate_7d),
                    trend_score = VALUES(trend_score), competition_score = VALUES(competition_score),
                    estimated_profit = VALUES(estimated_profit), estimated_profit_margin = VALUES(estimated_profit_margin),
                    profit_score = VALUES(profit_score), risk_score = VALUES(risk_score), selection_score = VALUES(selection_score),
                    lifecycle_stage = VALUES(lifecycle_stage), recommendation = VALUES(recommendation),
                    score_details_json = VALUES(score_details_json), reasons_json = VALUES(reasons_json), risks_json = VALUES(risks_json),
                    source_data_updated_at = VALUES(source_data_updated_at), calculated_at = VALUES(calculated_at)
                """, parameters);
    }

    @Override
    public void schedule(long productId, LocalDate analysisDate, String algorithmVersion, Instant now) {
        jdbc.update("""
                INSERT INTO analysis_job (
                    product_id, analysis_date, algorithm_version, status, attempt_count, next_retry_at, created_at, updated_at
                ) VALUES (:productId, :analysisDate, :algorithmVersion, 'PENDING', 0, :now, :now, :now)
                ON DUPLICATE KEY UPDATE
                    status = IF(status = 'RUNNING', 'RUNNING', 'PENDING'), next_retry_at = IF(status = 'RUNNING', next_retry_at, VALUES(next_retry_at)),
                    error_message = NULL, updated_at = VALUES(updated_at)
                """, Map.of("productId", productId, "analysisDate", analysisDate, "algorithmVersion", algorithmVersion, "now", now));
    }

    @Override
    public List<AnalysisTarget> findRecalculationTargets(String algorithmVersion, int limit) {
        return jdbc.query("""
                SELECT product.id, product.market, product.category_id, product.latest_stat_date
                  FROM product product
                 WHERE product.status = 'ACTIVE' AND product.latest_stat_date IS NOT NULL
                   AND NOT EXISTS (
                        SELECT 1
                          FROM product_analysis_snapshot snapshot
                         WHERE snapshot.product_id = product.id
                           AND snapshot.analysis_date = product.latest_stat_date
                           AND snapshot.algorithm_version = :algorithmVersion
                           AND snapshot.source_data_updated_at >= (
                                SELECT MAX(stat.updated_at) FROM product_daily_stat stat WHERE stat.product_id = product.id
                           )
                   )
                 ORDER BY product.id ASC
                 LIMIT :limit
                """, Map.of("algorithmVersion", algorithmVersion, "limit", limit), (resultSet, rowNumber) -> new AnalysisTarget(
                resultSet.getLong("id"), resultSet.getString("market"), resultSet.getLong("category_id"),
                resultSet.getObject("latest_stat_date", LocalDate.class)));
    }

    @Override
    public List<AnalysisJob> findRunnableJobs(Instant now, int limit) {
        return jdbc.query("""
                SELECT id, product_id, analysis_date, algorithm_version, status, attempt_count, next_retry_at
                  FROM analysis_job
                 WHERE status = 'PENDING' AND next_retry_at <= :now
                 ORDER BY id ASC LIMIT :limit
                """, Map.of("now", now, "limit", limit), (resultSet, rowNumber) -> new AnalysisJob(
                resultSet.getLong("id"), resultSet.getLong("product_id"), resultSet.getObject("analysis_date", LocalDate.class),
                resultSet.getString("algorithm_version"), AnalysisJob.Status.valueOf(resultSet.getString("status")),
                resultSet.getInt("attempt_count"), resultSet.getTimestamp("next_retry_at").toInstant()));
    }

    @Override
    public boolean claim(long jobId, Instant now) {
        return jdbc.update("""
                UPDATE analysis_job SET status = 'RUNNING', attempt_count = attempt_count + 1, started_at = :now, updated_at = :now
                 WHERE id = :jobId AND status = 'PENDING' AND next_retry_at <= :now
                """, Map.of("jobId", jobId, "now", now)) == 1;
    }

    @Override
    public void complete(long jobId, Instant now) {
        jdbc.update("""
                UPDATE analysis_job SET status = 'SUCCESS', completed_at = :now, error_message = NULL, updated_at = :now WHERE id = :jobId
                """, Map.of("jobId", jobId, "now", now));
    }

    @Override
    public void retryOrFail(long jobId, int attempts, String message, Instant nextRetryAt, Instant now) {
        jdbc.update("""
                UPDATE analysis_job
                   SET status = :status, error_message = :message, next_retry_at = :nextRetryAt, updated_at = :now,
                       completed_at = CASE WHEN :status = 'FAILED' THEN :now ELSE completed_at END
                 WHERE id = :jobId
                """, Map.of("jobId", jobId, "status", attempts >= 3 ? "FAILED" : "PENDING", "message", message,
                "nextRetryAt", nextRetryAt, "now", now));
    }

    @Override
    public Instant latestSourceUpdate(long productId) {
        Timestamp timestamp = jdbc.queryForObject("SELECT MAX(updated_at) FROM product_daily_stat WHERE product_id = :productId",
                Map.of("productId", productId), Timestamp.class);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private Optional<CostProfile> findCost(String sql, Map<String, Object> parameters) {
        return jdbc.query(sql, parameters, (resultSet, rowNumber) -> cost(resultSet)).stream().findFirst();
    }

    private Optional<AnalysisResult> findSnapshot(String sql, Map<String, Object> parameters) {
        return jdbc.query(sql, parameters, (resultSet, rowNumber) -> snapshot(resultSet)).stream().findFirst();
    }

    private CostProfile cost(ResultSet resultSet) throws SQLException {
        return new CostProfile(resultSet.getString("currency"), resultSet.getBigDecimal("purchase_cost"),
                resultSet.getBigDecimal("domestic_shipping_cost"), resultSet.getBigDecimal("international_shipping_cost"),
                resultSet.getBigDecimal("platform_commission_rate"), resultSet.getBigDecimal("payment_fee_rate"),
                resultSet.getBigDecimal("advertising_cost_rate"), resultSet.getBigDecimal("refund_loss_rate"), resultSet.getBigDecimal("other_cost"));
    }

    private AnalysisDailyStat stat(ResultSet resultSet) throws SQLException {
        return new AnalysisDailyStat(resultSet.getObject("stat_date", LocalDate.class), resultSet.getBigDecimal("price"),
                nullableLong(resultSet, "sales_volume"), nullableLong(resultSet, "video_count"), nullableLong(resultSet, "creator_count"),
                nullableLong(resultSet, "shop_count"), nullableLong(resultSet, "similar_product_count"),
                resultSet.getBigDecimal("top10_shop_sales_share"), resultSet.getBigDecimal("top10_creator_sales_share"),
                resultSet.getBigDecimal("negative_review_rate"), resultSet.getBigDecimal("rating"));
    }

    private AnalysisResult snapshot(ResultSet resultSet) throws SQLException {
        BigDecimal estimatedProfit = resultSet.getBigDecimal("estimated_profit");
        BigDecimal estimatedProfitMargin = resultSet.getBigDecimal("estimated_profit_margin");
        ProfitCalculation profit = estimatedProfit == null ? null : new ProfitCalculation(null, null, null, null, null, null,
                estimatedProfit, estimatedProfitMargin, null);
        String lifecycle = resultSet.getString("lifecycle_stage");
        return new AnalysisResult(resultSet.getLong("product_id"), resultSet.getString("market"), resultSet.getLong("category_id"),
                resultSet.getObject("analysis_date", LocalDate.class), resultSet.getString("algorithm_version"),
                ScoreStatus.valueOf(resultSet.getString("score_status")), nullableLong(resultSet, "sales_volume_7d"),
                resultSet.getBigDecimal("sales_growth_rate_7d"), resultSet.getBigDecimal("sales_growth_rate_30d"),
                resultSet.getBigDecimal("video_growth_rate_7d"), resultSet.getBigDecimal("creator_growth_rate_7d"),
                resultSet.getBigDecimal("trend_score"), resultSet.getBigDecimal("competition_score"), profit,
                resultSet.getBigDecimal("profit_score"), resultSet.getBigDecimal("risk_score"), resultSet.getBigDecimal("selection_score"),
                lifecycle == null ? null : LifecycleStage.valueOf(lifecycle), Recommendation.valueOf(resultSet.getString("recommendation")),
                read(resultSet.getString("score_details_json"), SCORE_DETAILS), read(resultSet.getString("reasons_json"), STRINGS),
                read(resultSet.getString("risks_json"), STRINGS));
    }

    private MapSqlParameterSource costParameters(long userId, long productId, CostProfile profile, Instant now) {
        return new MapSqlParameterSource().addValue("userId", userId).addValue("productId", productId).addValue("currency", profile.currency())
                .addValue("purchaseCost", profile.purchaseCost()).addValue("domesticShippingCost", profile.domesticShippingCost())
                .addValue("internationalShippingCost", profile.internationalShippingCost()).addValue("platformCommissionRate", profile.platformCommissionRate())
                .addValue("paymentFeeRate", profile.paymentFeeRate()).addValue("advertisingCostRate", profile.advertisingCostRate())
                .addValue("refundLossRate", profile.refundLossRate()).addValue("otherCost", profile.otherCost()).addValue("now", now);
    }

    private Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize analysis snapshot", exception);
        }
    }

    private <T> List<T> read(String value, TypeReference<List<T>> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to read analysis snapshot", exception);
        }
    }
}
