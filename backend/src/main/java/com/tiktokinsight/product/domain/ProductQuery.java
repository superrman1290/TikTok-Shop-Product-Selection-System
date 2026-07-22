package com.tiktokinsight.product.domain;

public record ProductQuery(
        int page,
        int pageSize,
        String market,
        Long categoryId,
        String keyword,
        ProductStatus status,
        ProductSort sort,
        SortDirection direction
) {
    public int offset() {
        return (page - 1) * pageSize;
    }
}
