package com.shopee.product.dto;

import com.shopee.product.entity.Inventory;

import java.util.UUID;

public record InventoryResponse(
        UUID variantId,
        UUID warehouseId,
        Integer quantityOnHand,
        Integer quantityReserved,
        Integer quantityAvailable,
        Long version
) {
    public static InventoryResponse from(Inventory inventory) {
        return new InventoryResponse(
                inventory.getVariantId(), inventory.getWarehouseId(),
                inventory.getQuantityOnHand(), inventory.getQuantityReserved(),
                inventory.getQuantityOnHand() - inventory.getQuantityReserved(),
                inventory.getVersion()
        );
    }
}
