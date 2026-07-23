package com.tiktokinsight.alert.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AlertRuleRequest(
        @NotNull Long productId,
        @DecimalMin(value = "0.00") BigDecimal salesGrowth7dThreshold,
        @DecimalMin(value = "0.00") BigDecimal priceDrop7dThreshold,
        @DecimalMin(value = "0.00") BigDecimal competitionScoreIncreaseThreshold,
        @DecimalMin(value = "0.00") BigDecimal selectionScoreDropThreshold,
        @NotNull Boolean enabled
) { }
