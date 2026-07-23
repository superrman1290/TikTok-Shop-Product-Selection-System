package com.tiktokinsight.alert.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AlertEvent(long id, long productId, String productTitle, String market, long alertRuleId,
                         AlertMetricType metricType, LocalDate statDate, BigDecimal metricValue, BigDecimal thresholdValue,
                         boolean readStatus, Instant readAt, Instant createdAt) { }
