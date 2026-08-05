package com.shopee.product.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record InventoryImportRequest(
        UUID warehouseId,
        @NotNull @Positive Integer quantity,
        String note
) {
}
