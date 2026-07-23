package com.tiktokinsight.product.api;

import com.tiktokinsight.common.api.ApiResponse;
import com.tiktokinsight.common.api.PageResponse;
import com.tiktokinsight.common.logging.RequestIdFilter;
import com.tiktokinsight.product.application.ProductCatalogService;
import com.tiktokinsight.product.domain.ProductDailyStat;
import com.tiktokinsight.product.domain.ProductDetail;
import com.tiktokinsight.product.domain.ProductStatus;
import com.tiktokinsight.product.domain.ProductSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductCatalogService service;

    public ProductController(ProductCatalogService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<ProductSummary>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String market,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String lifecycleStage,
            @RequestParam(required = false) String recommendation,
            @RequestParam(required = false) BigDecimal minSelectionScore,
            @RequestParam(required = false) BigDecimal maxSelectionScore,
            @RequestParam(defaultValue = "collectedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return ApiResponse.success(service.list(service.query(
                page, pageSize, market, categoryId, keyword, ProductStatus.ACTIVE, minPrice, maxPrice, lifecycleStage, recommendation, minSelectionScore, maxSelectionScore, sortBy, direction
        )), RequestIdFilter.currentRequestId());
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductDetail> get(@PathVariable long productId) {
        return ApiResponse.success(service.get(productId, false), RequestIdFilter.currentRequestId());
    }

    @GetMapping("/{productId}/stats")
    public ApiResponse<List<ProductDailyStat>> stats(
            @PathVariable long productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ApiResponse.success(service.stats(productId, startDate, endDate), RequestIdFilter.currentRequestId());
    }
}
