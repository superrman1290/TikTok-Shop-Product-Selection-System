package com.tiktokinsight.alert.domain;

import com.tiktokinsight.common.api.PageResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AlertRepository {
    boolean activeProductExists(long productId);
    List<AlertRule> findRules(long userId);
    Optional<AlertRule> findRule(long userId, long ruleId);
    AlertRule saveRule(long userId, Long ruleId, AlertRule rule, Instant now);
    boolean deleteRule(long userId, long ruleId);
    List<AlertEvaluationTarget> findEnabledEvaluationTargets(LocalDate statDate, String algorithmVersion);
    boolean createEvent(long userId, long productId, long ruleId, AlertMetricType type, LocalDate statDate, java.math.BigDecimal metricValue, java.math.BigDecimal thresholdValue, Instant now);
    PageResponse<AlertEvent> findEvents(long userId, String market, Boolean readStatus, int page, int pageSize);
    boolean markRead(long userId, long alertId, Instant now);
    int markAllRead(long userId, Instant now);
    long countUnread(long userId);
}
