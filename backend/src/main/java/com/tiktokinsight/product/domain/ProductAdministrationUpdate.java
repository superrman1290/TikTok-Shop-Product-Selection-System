package com.tiktokinsight.product.domain;

import java.math.BigDecimal;

public record ProductAdministrationUpdate(
        String title,
        BigDecimal currentPrice,
        BigDecimal originalPrice,
        String imageUrl,
        String productUrl,
        ProductStatus status
) {
}
