package com.tiktokinsight.analysis.application;

import com.tiktokinsight.analysis.domain.AnalysisProduct;
import com.tiktokinsight.analysis.domain.AnalysisRepository;
import com.tiktokinsight.analysis.domain.CostProfile;
import com.tiktokinsight.analysis.domain.ProfitCalculation;
import com.tiktokinsight.analysis.domain.SelectionV1Algorithm;
import com.tiktokinsight.common.api.ApiErrorCode;
import com.tiktokinsight.common.exception.ApiException;
import java.math.BigDecimal;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CostProfileService {

    private final AnalysisRepository repository;
    private final Clock clock;
    private final SelectionV1Algorithm algorithm = new SelectionV1Algorithm();

    public CostProfileService(AnalysisRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ResolvedCostProfile get(long userId, long productId) {
        AnalysisProduct product = product(productId);
        return repository.findUserCost(userId, productId)
                .map(profile -> new ResolvedCostProfile(profile, "USER"))
                .or(() -> repository.findMarketCost(product.market()).map(profile -> new ResolvedCostProfile(profile, "MARKET")))
                .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, ApiErrorCode.COST_PROFILE_MISSING));
    }

    @Transactional
    public CostProfile save(long userId, long productId, CostProfile profile) {
        AnalysisProduct product = product(productId);
        validate(product, profile);
        repository.saveUserCost(userId, productId, profile, clock.instant());
        return profile;
    }

    @Transactional
    public void delete(long userId, long productId) {
        product(productId);
        repository.deleteUserCost(userId, productId);
    }

    @Transactional(readOnly = true)
    public ProfitCalculation calculate(long userId, long productId, BigDecimal sellingPrice, CostProfile requestProfile) {
        AnalysisProduct product = product(productId);
        CostProfile profile = requestProfile == null ? get(userId, productId).profile() : requestProfile;
        validate(product, profile);
        try {
            return algorithm.profit(sellingPrice, profile);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED);
        }
    }

    private AnalysisProduct product(long productId) {
        return repository.findProduct(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.PRODUCT_NOT_FOUND));
    }

    private void validate(AnalysisProduct product, CostProfile profile) {
        if (profile == null || !product.currency().equals(profile.currency()) || values(profile).stream().anyMatch(value -> value == null || value.compareTo(BigDecimal.ZERO) < 0)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED);
        }
    }

    private java.util.List<BigDecimal> values(CostProfile profile) {
        return java.util.Arrays.asList(profile.purchaseCost(), profile.domesticShippingCost(), profile.internationalShippingCost(),
                profile.platformCommissionRate(), profile.paymentFeeRate(), profile.advertisingCostRate(), profile.refundLossRate(), profile.otherCost());
    }

    public record ResolvedCostProfile(CostProfile profile, String source) {
    }
}
