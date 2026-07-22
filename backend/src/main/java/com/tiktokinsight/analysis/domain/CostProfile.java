package com.tiktokinsight.analysis.domain;

import java.math.BigDecimal;

public record CostProfile(
        String currency,
        BigDecimal purchaseCost,
        BigDecimal domesticShippingCost,
        BigDecimal internationalShippingCost,
        BigDecimal platformCommissionRate,
        BigDecimal paymentFeeRate,
        BigDecimal advertisingCostRate,
        BigDecimal refundLossRate,
        BigDecimal otherCost
) {
}
