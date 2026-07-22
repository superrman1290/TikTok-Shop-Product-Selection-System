package com.tiktokinsight.analysis.dto;

import com.tiktokinsight.analysis.domain.CostProfile;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CostProfileRequest(
        @NotBlank String currency,
        @NotNull @DecimalMin("0.00") BigDecimal purchaseCost,
        @NotNull @DecimalMin("0.00") BigDecimal domesticShippingCost,
        @NotNull @DecimalMin("0.00") BigDecimal internationalShippingCost,
        @NotNull @DecimalMin("0.00") BigDecimal platformCommissionRate,
        @NotNull @DecimalMin("0.00") BigDecimal paymentFeeRate,
        @NotNull @DecimalMin("0.00") BigDecimal advertisingCostRate,
        @NotNull @DecimalMin("0.00") BigDecimal refundLossRate,
        @NotNull @DecimalMin("0.00") BigDecimal otherCost
) {
    public CostProfile toDomain() {
        return new CostProfile(currency.trim().toUpperCase(), purchaseCost, domesticShippingCost, internationalShippingCost,
                platformCommissionRate, paymentFeeRate, advertisingCostRate, refundLossRate, otherCost);
    }
}
