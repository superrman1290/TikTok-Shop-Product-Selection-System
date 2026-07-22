package com.tiktokinsight.analysis.domain;

import java.math.BigDecimal;

public record ScoreDetail(
        String metricCode,
        String metricName,
        BigDecimal rawValue,
        BigDecimal normalizedScore,
        BigDecimal weight,
        BigDecimal parentWeight,
        BigDecimal overallContribution,
        String description
) {
}
