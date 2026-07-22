package com.tiktokinsight.analysis.api;

import com.tiktokinsight.analysis.application.AnalysisApplicationService;
import com.tiktokinsight.analysis.application.CostProfileService;
import com.tiktokinsight.analysis.application.CostProfileService.ResolvedCostProfile;
import com.tiktokinsight.analysis.domain.AnalysisResult;
import com.tiktokinsight.analysis.domain.CostProfile;
import com.tiktokinsight.analysis.domain.ProfitCalculation;
import com.tiktokinsight.analysis.dto.CostProfileRequest;
import com.tiktokinsight.analysis.dto.ProfitCalculationRequest;
import com.tiktokinsight.analysis.dto.ProductAnalysisResponse;
import com.tiktokinsight.auth.application.AccessPrincipal;
import com.tiktokinsight.common.api.ApiResponse;
import com.tiktokinsight.common.logging.RequestIdFilter;
import com.tiktokinsight.product.application.ProductCatalogService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products/{productId}")
public class ProductAnalysisController {

    private final AnalysisApplicationService analysisService;
    private final CostProfileService costProfileService;
    private final ProductCatalogService productCatalogService;

    public ProductAnalysisController(
            AnalysisApplicationService analysisService,
            CostProfileService costProfileService,
            ProductCatalogService productCatalogService
    ) {
        this.analysisService = analysisService;
        this.costProfileService = costProfileService;
        this.productCatalogService = productCatalogService;
    }

    @GetMapping("/analysis")
    public ApiResponse<ProductAnalysisResponse> analysis(
            @PathVariable long productId,
            @RequestParam(required = false) String algorithmVersion
    ) {
        AnalysisResult result = analysisService.latest(productId, algorithmVersion);
        return ApiResponse.success(ProductAnalysisResponse.from(result, productCatalogService.get(productId, false)), RequestIdFilter.currentRequestId());
    }

    @GetMapping("/cost-profile")
    public ApiResponse<ResolvedCostProfile> costProfile(
            @PathVariable long productId,
            @AuthenticationPrincipal AccessPrincipal principal
    ) {
        return ApiResponse.success(costProfileService.get(principal.userId(), productId), RequestIdFilter.currentRequestId());
    }

    @PutMapping("/cost-profile")
    public ApiResponse<CostProfile> saveCostProfile(
            @PathVariable long productId,
            @AuthenticationPrincipal AccessPrincipal principal,
            @Valid @RequestBody CostProfileRequest request
    ) {
        return ApiResponse.success(costProfileService.save(principal.userId(), productId, request.toDomain()), RequestIdFilter.currentRequestId());
    }

    @DeleteMapping("/cost-profile")
    public ApiResponse<Void> deleteCostProfile(
            @PathVariable long productId,
            @AuthenticationPrincipal AccessPrincipal principal
    ) {
        costProfileService.delete(principal.userId(), productId);
        return ApiResponse.success(null, RequestIdFilter.currentRequestId());
    }

    @PostMapping("/profit-calculations")
    public ApiResponse<ProfitCalculation> profitCalculation(
            @PathVariable long productId,
            @AuthenticationPrincipal AccessPrincipal principal,
            @Valid @RequestBody ProfitCalculationRequest request
    ) {
        CostProfile customProfile = request.costProfile() == null ? null : request.costProfile().toDomain();
        return ApiResponse.success(costProfileService.calculate(principal.userId(), productId, request.sellingPrice(), customProfile),
                RequestIdFilter.currentRequestId());
    }
}
