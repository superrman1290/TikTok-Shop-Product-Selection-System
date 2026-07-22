package com.tiktokinsight.analysis.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ProfitCalculationRequest(
        @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal sellingPrice,
        @Valid CostProfileRequest costProfile
) {
}
