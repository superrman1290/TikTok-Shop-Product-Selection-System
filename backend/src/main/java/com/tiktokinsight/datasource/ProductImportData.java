package com.tiktokinsight.datasource;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductImportData(
        String externalProductId,
        String platform,
        String market,
        String title,
        String categoryExternalId,
        String categoryName,
        String shopExternalId,
        String shopName,
        String currency,
        BigDecimal currentPrice,
        BigDecimal originalPrice,
        String imageUrl,
        String productUrl,
        BigDecimal rating,
        long reviewCount,
        Instant listedAt,
        Instant collectedAt
) {
}
