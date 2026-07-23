package com.tiktokinsight.product.api;

import com.tiktokinsight.common.api.ApiResponse;
import com.tiktokinsight.audit.application.AuditService;
import com.tiktokinsight.auth.application.AccessPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.tiktokinsight.common.api.PageResponse;
import com.tiktokinsight.common.logging.RequestIdFilter;
import com.tiktokinsight.product.application.ProductCatalogService;
import com.tiktokinsight.product.domain.ProductDetail;
import com.tiktokinsight.product.domain.ProductStatus;
import com.tiktokinsight.product.domain.ProductSummary;
import com.tiktokinsight.product.dto.ProductAdministrationRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/products")
public class ProductAdministrationController {

    private final ProductCatalogService service;
    private final AuditService auditService;

    public ProductAdministrationController(ProductCatalogService service, AuditService auditService) {
        this.service = service;
        this.auditService = auditService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ProductSummary>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String market,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "collectedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return ApiResponse.success(service.list(ProductCatalogService.query(
                page, pageSize, market, categoryId, keyword, status, sortBy, direction
        )), RequestIdFilter.currentRequestId());
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductDetail> get(@PathVariable long productId) {
        return ApiResponse.success(service.get(productId, true), RequestIdFilter.currentRequestId());
    }

    @PutMapping("/{productId}")
    public ApiResponse<ProductDetail> update(
            @PathVariable long productId,
            @Valid @RequestBody ProductAdministrationRequest request,
            @AuthenticationPrincipal AccessPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        var product = service.update(productId, request.toUpdate());
        auditService.record(principal, "PRODUCT_UPDATED", "PRODUCT", String.valueOf(productId), "SUCCESS", "{\"title\":\"" + product.title() + "\"}", servletRequest);
        return ApiResponse.success(product, RequestIdFilter.currentRequestId());
    }
}
