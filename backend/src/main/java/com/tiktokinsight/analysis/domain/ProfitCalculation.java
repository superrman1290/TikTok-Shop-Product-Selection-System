package com.tiktokinsight.analysis.domain;

import java.math.BigDecimal;

public record ProfitCalculation(
        BigDecimal sellingPrice,
        BigDecimal platformCommission,
        BigDecimal paymentFee,
        BigDecimal advertisingCost,
        BigDecimal refundLoss,
        BigDecimal totalCost,
        BigDecimal estimatedProfit,
        BigDecimal estimatedProfitMargin,
        BigDecimal costProfitMargin
) {
}
