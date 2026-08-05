package com.shopee.product.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record VariantInitialStockRequest(
        UUID warehouseId,
        @NotNull @PositiveOrZero Integer quantity
) {
}
