package com.tiktokinsight.product.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductSummary(
        long id,
        String externalProductId,
        String platform,
        String market,
        String title,
        long categoryId,
        String categoryName,
        long shopId,
        String shopName,
        String currency,
        BigDecimal currentPrice,
        BigDecimal originalPrice,
        String imageUrl,
        String productUrl,
        BigDecimal rating,
        long reviewCount,
        Instant listedAt,
        Instant collectedAt,
        ProductStatus status
) {
}
