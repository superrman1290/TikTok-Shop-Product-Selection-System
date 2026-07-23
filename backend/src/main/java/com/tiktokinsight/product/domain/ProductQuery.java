package com.tiktokinsight.product.domain;

import java.math.BigDecimal;

public record ProductQuery(
        int page,
        int pageSize,
        String market,
        Long categoryId,
        String keyword,
        ProductStatus status,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String lifecycleStage,
        String recommendation,
        BigDecimal minSelectionScore,
        BigDecimal maxSelectionScore,
        String algorithmVersion,
        ProductSort sort,
        SortDirection direction
) {
    public int offset() {
        return (page - 1) * pageSize;
    }
}
