package com.shopee.product.repository;

import com.shopee.product.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
    Optional<Inventory> findByVariantIdAndWarehouseId(UUID variantId, UUID warehouseId);
    List<Inventory> findByVariantIdIn(Collection<UUID> variantIds);
}
