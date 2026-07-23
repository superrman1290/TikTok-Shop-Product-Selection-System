package com.tiktokinsight.alert.infrastructure;

import com.tiktokinsight.alert.domain.AlertEvaluationTarget;
import com.tiktokinsight.alert.domain.AlertEvent;
import com.tiktokinsight.alert.domain.AlertMetricType;
import com.tiktokinsight.alert.domain.AlertRepository;
import com.tiktokinsight.alert.domain.AlertRule;
import com.tiktokinsight.common.api.PageResponse;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAlertRepository implements AlertRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcAlertRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public boolean activeProductExists(long productId) { Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM product WHERE id = :productId AND status = 'ACTIVE'", Map.of("productId", productId), Integer.class); return count != null && count > 0; }
    @Override public List<AlertRule> findRules(long userId) { return jdbc.query("SELECT * FROM alert_rule WHERE user_id = :userId ORDER BY created_at DESC, id DESC", Map.of("userId", userId), (rs, row) -> rule(rs)); }
    @Override public Optional<AlertRule> findRule(long userId, long ruleId) { return jdbc.query("SELECT * FROM alert_rule WHERE id = :ruleId AND user_id = :userId", Map.of("ruleId", ruleId, "userId", userId), (rs, row) -> rule(rs)).stream().findFirst(); }
    @Override public AlertRule saveRule(long userId, Long ruleId, AlertRule rule, Instant now) {
        if (ruleId == null) {
            var keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
            jdbc.update("""
                    INSERT INTO alert_rule (user_id, product_id, sales_growth_7d_threshold, price_drop_7d_threshold, competition_score_increase_threshold, selection_score_drop_threshold, enabled, created_at, updated_at)
                    VALUES (:userId, :productId, :salesGrowth, :priceDrop, :competitionIncrease, :selectionDrop, :enabled, :now, :now)
                    """, params(userId, rule, now), keyHolder, new String[]{"id"});
            return findRule(userId, keyHolder.getKey().longValue()).orElseThrow();
        }
        jdbc.update("""
                UPDATE alert_rule SET product_id = :productId, sales_growth_7d_threshold = :salesGrowth, price_drop_7d_threshold = :priceDrop,
                  competition_score_increase_threshold = :competitionIncrease, selection_score_drop_threshold = :selectionDrop, enabled = :enabled, updated_at = :now
                WHERE id = :ruleId AND user_id = :userId
                """, params(userId, rule, now).addValue("ruleId", ruleId));
        return findRule(userId, ruleId).orElseThrow();
    }
    @Override public boolean deleteRule(long userId, long ruleId) { return jdbc.update("DELETE FROM alert_rule WHERE id = :ruleId AND user_id = :userId", Map.of("ruleId", ruleId, "userId", userId)) > 0; }
    @Override public List<AlertEvaluationTarget> findEnabledEvaluationTargets(LocalDate statDate, String algorithmVersion) {
        return jdbc.query("""
                SELECT r.id AS rule_id, r.user_id, r.product_id, current_snapshot.sales_growth_rate_7d, current_snapshot.competition_score, current_snapshot.selection_score,
                       previous_snapshot.competition_score AS previous_competition_score, previous_snapshot.selection_score AS previous_selection_score,
                       current_stat.price AS current_price, previous_stat.price AS previous_price,
                       r.sales_growth_7d_threshold, r.price_drop_7d_threshold, r.competition_score_increase_threshold, r.selection_score_drop_threshold
                  FROM alert_rule r
                  JOIN product p ON p.id = r.product_id AND p.latest_stat_date = :statDate
                  JOIN product_analysis_snapshot current_snapshot ON current_snapshot.product_id = r.product_id AND current_snapshot.analysis_date = :statDate AND current_snapshot.algorithm_version = :version
                  LEFT JOIN product_analysis_snapshot previous_snapshot ON previous_snapshot.product_id = r.product_id AND previous_snapshot.analysis_date = DATE_SUB(:statDate, INTERVAL 7 DAY) AND previous_snapshot.algorithm_version = :version
                  LEFT JOIN product_daily_stat current_stat ON current_stat.product_id = r.product_id AND current_stat.stat_date = :statDate
                  LEFT JOIN product_daily_stat previous_stat ON previous_stat.product_id = r.product_id AND previous_stat.stat_date = DATE_SUB(:statDate, INTERVAL 7 DAY)
                 WHERE r.enabled = TRUE
                """, Map.of("statDate", statDate, "version", algorithmVersion), (rs, row) -> new AlertEvaluationTarget(
                rs.getLong("rule_id"), rs.getLong("user_id"), rs.getLong("product_id"), statDate,
                rs.getBigDecimal("sales_growth_rate_7d"), rs.getBigDecimal("current_price"), rs.getBigDecimal("previous_price"),
                rs.getBigDecimal("competition_score"), rs.getBigDecimal("previous_competition_score"), rs.getBigDecimal("selection_score"), rs.getBigDecimal("previous_selection_score"),
                rs.getBigDecimal("sales_growth_7d_threshold"), rs.getBigDecimal("price_drop_7d_threshold"), rs.getBigDecimal("competition_score_increase_threshold"), rs.getBigDecimal("selection_score_drop_threshold")));
    }
    @Override public boolean createEvent(long userId, long productId, long ruleId, AlertMetricType type, LocalDate statDate, BigDecimal metricValue, BigDecimal thresholdValue, Instant now) {
        try { return jdbc.update("""
                INSERT INTO alert_event (user_id, product_id, alert_rule_id, metric_type, stat_date, metric_value, threshold_value, read_status, created_at)
                VALUES (:userId, :productId, :ruleId, :metricType, :statDate, :metricValue, :thresholdValue, FALSE, :now)
                """, new MapSqlParameterSource().addValue("userId", userId).addValue("productId", productId).addValue("ruleId", ruleId).addValue("metricType", type.name()).addValue("statDate", statDate).addValue("metricValue", metricValue).addValue("thresholdValue", thresholdValue).addValue("now", now)) == 1; } catch (DuplicateKeyException exception) { return false; }
    }
    @Override public PageResponse<AlertEvent> findEvents(long userId, String market, Boolean readStatus, int page, int pageSize) {
        var where = new StringBuilder(" WHERE event.user_id = :userId"); Map<String, Object> params = new HashMap<>(); params.put("userId", userId);
        if (market != null) { where.append(" AND product.market = :market"); params.put("market", market); } if (readStatus != null) { where.append(" AND event.read_status = :readStatus"); params.put("readStatus", readStatus); }
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM alert_event event JOIN product product ON product.id = event.product_id" + where, params, Long.class);
        params.put("limit", pageSize); params.put("offset", (page - 1) * pageSize);
        List<AlertEvent> items = jdbc.query("SELECT event.*, product.title AS product_title, product.market FROM alert_event event JOIN product product ON product.id = event.product_id" + where + " ORDER BY event.created_at DESC, event.id DESC LIMIT :limit OFFSET :offset", params, (rs, row) -> event(rs));
        return new PageResponse<>(page, pageSize, total == null ? 0 : total, items);
    }
    @Override public boolean markRead(long userId, long alertId, Instant now) { return jdbc.update("UPDATE alert_event SET read_status = TRUE, read_at = :now WHERE id = :alertId AND user_id = :userId", Map.of("alertId", alertId, "userId", userId, "now", now)) > 0; }
    @Override public int markAllRead(long userId, Instant now) { return jdbc.update("UPDATE alert_event SET read_status = TRUE, read_at = :now WHERE user_id = :userId AND read_status = FALSE", Map.of("userId", userId, "now", now)); }
    @Override public long countUnread(long userId) { Long count = jdbc.queryForObject("SELECT COUNT(*) FROM alert_event WHERE user_id = :userId AND read_status = FALSE", Map.of("userId", userId), Long.class); return count == null ? 0 : count; }
    private AlertRule rule(java.sql.ResultSet rs) throws java.sql.SQLException { return new AlertRule(rs.getLong("id"), rs.getLong("product_id"), rs.getBigDecimal("sales_growth_7d_threshold"), rs.getBigDecimal("price_drop_7d_threshold"), rs.getBigDecimal("competition_score_increase_threshold"), rs.getBigDecimal("selection_score_drop_threshold"), rs.getBoolean("enabled"), instant(rs, "created_at"), instant(rs, "updated_at")); }
    private AlertEvent event(java.sql.ResultSet rs) throws java.sql.SQLException { return new AlertEvent(rs.getLong("id"), rs.getLong("product_id"), rs.getString("product_title"), rs.getString("market"), rs.getLong("alert_rule_id"), AlertMetricType.valueOf(rs.getString("metric_type")), rs.getObject("stat_date", LocalDate.class), rs.getBigDecimal("metric_value"), rs.getBigDecimal("threshold_value"), rs.getBoolean("read_status"), instant(rs, "read_at"), instant(rs, "created_at")); }
    private Instant instant(java.sql.ResultSet rs, String column) throws java.sql.SQLException { Timestamp value = rs.getTimestamp(column); return value == null ? null : value.toInstant(); }
    private MapSqlParameterSource params(long userId, AlertRule rule, Instant now) { return new MapSqlParameterSource().addValue("userId", userId).addValue("productId", rule.productId()).addValue("salesGrowth", rule.salesGrowth7dThreshold()).addValue("priceDrop", rule.priceDrop7dThreshold()).addValue("competitionIncrease", rule.competitionScoreIncreaseThreshold()).addValue("selectionDrop", rule.selectionScoreDropThreshold()).addValue("enabled", rule.enabled()).addValue("now", now); }
}
