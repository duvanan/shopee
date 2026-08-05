package com.shopee.product.repository;

import com.shopee.product.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {
    List<InventoryTransaction> findByVariantIdOrderByCreatedAtDesc(UUID variantId);
}
