package com.tiktokinsight.product.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ProductDetail(
        long id,
        String externalProductId,
        String platform,
        String market,
        String title,
        long categoryId,
        String categoryExternalId,
        String categoryName,
        long shopId,
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
        Instant collectedAt,
        LocalDate latestStatDate,
        ProductStatus status
) {
}
