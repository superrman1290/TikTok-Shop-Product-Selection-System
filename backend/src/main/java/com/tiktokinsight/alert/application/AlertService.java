package com.tiktokinsight.alert.application;

import com.tiktokinsight.alert.domain.AlertEvaluationTarget;
import com.tiktokinsight.alert.domain.AlertEvent;
import com.tiktokinsight.alert.domain.AlertMetricType;
import com.tiktokinsight.alert.domain.AlertRepository;
import com.tiktokinsight.common.api.ApiErrorCode;
import com.tiktokinsight.common.api.PageResponse;
import com.tiktokinsight.common.exception.ApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AlertService {
    private static final BigDecimal MIN_PRICE = new BigDecimal("0.01");
    private final AlertRepository repository;
    private final Clock clock;
    private final String activeVersion;
    public AlertService(AlertRepository repository, Clock clock, @Value("${app.analysis.active-version:selection-v1.0}") String activeVersion) { this.repository = repository; this.clock = clock; this.activeVersion = activeVersion; }
    public void evaluate(LocalDate statDate) { repository.findEnabledEvaluationTargets(statDate, activeVersion).forEach(this::evaluateTarget); }
    public PageResponse<AlertEvent> list(long userId, String market, Boolean readStatus, int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED);
        return repository.findEvents(userId, market == null || market.isBlank() ? null : market.trim().toUpperCase(), readStatus, page, pageSize);
    }
    public void markRead(long userId, long alertId) { if (!repository.markRead(userId, alertId, clock.instant())) throw new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.ALERT_NOT_FOUND); }
    public int markAllRead(long userId) { return repository.markAllRead(userId, clock.instant()); }
    public long unreadCount(long userId) { return repository.countUnread(userId); }
    private void evaluateTarget(AlertEvaluationTarget target) {
        trigger(target, AlertMetricType.SALES_GROWTH_7D, target.salesGrowthRate7d(), target.salesGrowth7dThreshold());
        if (target.currentPrice() != null && target.priceSevenDaysAgo() != null) {
            BigDecimal base = target.priceSevenDaysAgo().max(MIN_PRICE);
            trigger(target, AlertMetricType.PRICE_DROP_7D, target.priceSevenDaysAgo().subtract(target.currentPrice()).multiply(BigDecimal.valueOf(100)).divide(base, 4, RoundingMode.HALF_UP), target.priceDrop7dThreshold());
        }
        if (target.competitionScore() != null && target.previousCompetitionScore() != null) trigger(target, AlertMetricType.COMPETITION_SCORE_INCREASE, target.competitionScore().subtract(target.previousCompetitionScore()), target.competitionScoreIncreaseThreshold());
        if (target.selectionScore() != null && target.previousSelectionScore() != null) trigger(target, AlertMetricType.SELECTION_SCORE_DROP, target.previousSelectionScore().subtract(target.selectionScore()), target.selectionScoreDropThreshold());
    }
    private void trigger(AlertEvaluationTarget target, AlertMetricType type, BigDecimal value, BigDecimal threshold) {
        if (value != null && threshold != null && value.compareTo(threshold) >= 0) repository.createEvent(target.userId(), target.productId(), target.ruleId(), type, target.statDate(), value, threshold, clock.instant());
    }
}
