package com.tiktokinsight.datasource;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductStatImportData(
        String externalProductId,
        String platform,
        String market,
        LocalDate statDate,
        BigDecimal price,
        long salesVolume,
        BigDecimal salesAmount,
        long totalSalesVolume,
        long videoCount,
        long creatorCount,
        long shopCount,
        long similarProductCount,
        BigDecimal top10ShopSalesShare,
        BigDecimal top10CreatorSalesShare,
        BigDecimal negativeReviewRate,
        long reviewCount,
        BigDecimal rating
) {
}
