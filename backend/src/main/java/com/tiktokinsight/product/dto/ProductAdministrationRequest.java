package com.tiktokinsight.product.dto;

import com.tiktokinsight.product.domain.ProductAdministrationUpdate;
import com.tiktokinsight.product.domain.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductAdministrationRequest(
        @NotBlank @Size(max = 500) String title,
        @NotNull @DecimalMin("0.00") BigDecimal currentPrice,
        @DecimalMin("0.00") BigDecimal originalPrice,
        @Size(max = 1500) String imageUrl,
        @Size(max = 1500) String productUrl,
        @NotNull ProductStatus status
) {
    public ProductAdministrationUpdate toUpdate() {
        return new ProductAdministrationUpdate(title.trim(), currentPrice, originalPrice, imageUrl, productUrl, status);
    }
}
