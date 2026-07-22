package com.tiktokinsight.product.application;

import com.tiktokinsight.common.api.ApiErrorCode;
import com.tiktokinsight.common.api.PageResponse;
import com.tiktokinsight.common.exception.ApiException;
import com.tiktokinsight.product.domain.ProductAdministrationUpdate;
import com.tiktokinsight.product.domain.ProductCatalogRepository;
import com.tiktokinsight.product.domain.ProductDailyStat;
import com.tiktokinsight.product.domain.ProductDetail;
import com.tiktokinsight.product.domain.ProductQuery;
import com.tiktokinsight.product.domain.ProductStatus;
import com.tiktokinsight.product.domain.ProductSummary;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ProductCatalogService {

    private final ProductCatalogRepository repository;

    public ProductCatalogService(ProductCatalogRepository repository) {
        this.repository = repository;
    }

    public PageResponse<ProductSummary> list(ProductQuery query) {
        return repository.findProducts(query);
    }

    public ProductDetail get(long productId, boolean includeInactive) {
        return repository.findById(productId, includeInactive)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.PRODUCT_NOT_FOUND));
    }

    public List<ProductDailyStat> stats(long productId, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED);
        }
        get(productId, false);
        return repository.findStats(productId, startDate, endDate);
    }

    public ProductDetail update(long productId, ProductAdministrationUpdate update) {
        return repository.update(productId, update)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.PRODUCT_NOT_FOUND));
    }

    public static ProductQuery query(
            int page,
            int pageSize,
            String market,
            Long categoryId,
            String keyword,
            ProductStatus status,
            String sortBy,
            String direction
    ) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED);
        }
        return new ProductQuery(
                page,
                pageSize,
                normalize(market),
                categoryId,
                keyword == null || keyword.isBlank() ? null : keyword.trim(),
                status,
                com.tiktokinsight.product.domain.ProductSort.fromApiValue(sortBy),
                com.tiktokinsight.product.domain.SortDirection.fromApiValue(direction)
        );
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase();
    }
}
