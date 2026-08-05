package com.shopee.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductVariantRequest(
        @NotBlank String sku,
        String barcode,
        @NotBlank String name,
        List<UUID> attributeValueIds,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal listPrice,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal salePrice,
        BigDecimal costPrice,
        BigDecimal weight,
        String weightUnit,
        BigDecimal length,
        BigDecimal width,
        BigDecimal height,
        String dimensionUnit,
        List<VariantInitialStockRequest> initialStocks
) {
}
