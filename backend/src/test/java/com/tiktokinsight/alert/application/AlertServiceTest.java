package com.tiktokinsight.alert.application;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tiktokinsight.alert.domain.AlertEvaluationTarget;
import com.tiktokinsight.alert.domain.AlertMetricType;
import com.tiktokinsight.alert.domain.AlertRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlertServiceTest {
    private final AlertRepository repository = mock(AlertRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-22T02:00:00Z"), ZoneOffset.UTC);
    private final AlertService service = new AlertService(repository, clock, "selection-v1.0");

    @Test
    void evaluatesPercentAndScorePointMetricsAgainstTheirConfiguredThresholds() {
        LocalDate date = LocalDate.of(2026, 7, 21);
        when(repository.findEnabledEvaluationTargets(date, "selection-v1.0")).thenReturn(List.of(target(date, new BigDecimal("90"), new BigDecimal("100"), new BigDecimal("30"), new BigDecimal("45"), new BigDecimal("80"), new BigDecimal("60"))));

        service.evaluate(date);

        verify(repository).createEvent(7L, 11L, 3L, AlertMetricType.SALES_GROWTH_7D, date, new BigDecimal("40"), new BigDecimal("30"), clock.instant());
        verify(repository).createEvent(7L, 11L, 3L, AlertMetricType.PRICE_DROP_7D, date, new BigDecimal("10.0000"), new BigDecimal("10"), clock.instant());
        verify(repository).createEvent(7L, 11L, 3L, AlertMetricType.COMPETITION_SCORE_INCREASE, date, new BigDecimal("15"), new BigDecimal("15"), clock.instant());
        verify(repository).createEvent(7L, 11L, 3L, AlertMetricType.SELECTION_SCORE_DROP, date, new BigDecimal("20"), new BigDecimal("20"), clock.instant());
    }

    @Test
    void skipsMetricsThatCannotBeComparedToSevenDayHistory() {
        LocalDate date = LocalDate.of(2026, 7, 21);
        when(repository.findEnabledEvaluationTargets(date, "selection-v1.0")).thenReturn(List.of(target(date, new BigDecimal("90"), null, null, new BigDecimal("45"), null, new BigDecimal("60"))));

        service.evaluate(date);

        verify(repository).createEvent(eq(7L), eq(11L), eq(3L), eq(AlertMetricType.SALES_GROWTH_7D), eq(date), eq(new BigDecimal("40")), eq(new BigDecimal("30")), eq(clock.instant()));
        verify(repository, never()).createEvent(eq(7L), eq(11L), eq(3L), eq(AlertMetricType.PRICE_DROP_7D), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(repository, never()).createEvent(eq(7L), eq(11L), eq(3L), eq(AlertMetricType.COMPETITION_SCORE_INCREASE), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(repository, never()).createEvent(eq(7L), eq(11L), eq(3L), eq(AlertMetricType.SELECTION_SCORE_DROP), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private AlertEvaluationTarget target(LocalDate date, BigDecimal currentPrice, BigDecimal previousPrice, BigDecimal previousCompetition, BigDecimal currentCompetition, BigDecimal previousSelection, BigDecimal currentSelection) {
        return new AlertEvaluationTarget(3L, 7L, 11L, date, new BigDecimal("40"), currentPrice, previousPrice, currentCompetition, previousCompetition, currentSelection, previousSelection, new BigDecimal("30"), new BigDecimal("10"), new BigDecimal("15"), new BigDecimal("20"));
    }
}
