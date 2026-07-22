package com.tiktokinsight.analysis.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AnalysisDailyStat(
        LocalDate statDate,
        BigDecimal price,
        Long salesVolume,
        Long videoCount,
        Long creatorCount,
        Long shopCount,
        Long similarProductCount,
        BigDecimal top10ShopSalesShare,
        BigDecimal top10CreatorSalesShare,
        BigDecimal negativeReviewRate,
        BigDecimal rating
) {
}
