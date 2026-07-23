package com.tiktokinsight.alert.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record AlertRule(long id, long productId, BigDecimal salesGrowth7dThreshold, BigDecimal priceDrop7dThreshold,
                        BigDecimal competitionScoreIncreaseThreshold, BigDecimal selectionScoreDropThreshold,
                        boolean enabled, Instant createdAt, Instant updatedAt) { }
