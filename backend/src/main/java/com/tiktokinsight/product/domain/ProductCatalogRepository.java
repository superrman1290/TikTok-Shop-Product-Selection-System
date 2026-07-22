package com.tiktokinsight.product.domain;

import com.tiktokinsight.common.api.PageResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductCatalogRepository {
    PageResponse<ProductSummary> findProducts(ProductQuery query);

    Optional<ProductDetail> findById(long productId, boolean includeInactive);

    List<ProductDailyStat> findStats(long productId, LocalDate startDate, LocalDate endDate);

    Optional<ProductDetail> update(long productId, ProductAdministrationUpdate update);
}
