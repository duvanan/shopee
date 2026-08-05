package com.shopee.product.service;

import com.shopee.common.exception.BusinessException;
import com.shopee.common.exception.ErrorCode;
import com.shopee.product.dto.InventoryImportRequest;
import com.shopee.product.dto.InventoryResponse;
import com.shopee.product.entity.Inventory;
import com.shopee.product.entity.InventoryTransaction;
import com.shopee.product.entity.InventoryTransactionType;
import com.shopee.product.entity.ProductVariant;
import com.shopee.product.entity.Warehouse;
import com.shopee.product.repository.InventoryRepository;
import com.shopee.product.repository.InventoryTransactionRepository;
import com.shopee.product.repository.ProductVariantRepository;
import com.shopee.product.repository.WarehouseRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Nhap kho co optimistic locking (Inventory.version) - 2 admin sua dong thoi thi nguoi sau nhan 409, xem docs/features/03-dynamic-product-catalog.md muc 5. */
@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ProductVariantRepository variantRepository;
    private final WarehouseRepository warehouseRepository;

    public InventoryService(InventoryRepository inventoryRepository,
                             InventoryTransactionRepository inventoryTransactionRepository,
                             ProductVariantRepository variantRepository,
                             WarehouseRepository warehouseRepository) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.variantRepository = variantRepository;
        this.warehouseRepository = warehouseRepository;
    }

    @Transactional
    public InventoryResponse importStock(UUID variantId, InventoryImportRequest request) {
        ProductVariant variant = variantRepository.findByIdAndDeletedAtIsNull(variantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VARIANT_NOT_FOUND));

        Warehouse warehouse = request.warehouseId() != null
                ? warehouseRepository.findById(request.warehouseId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy kho"))
                : warehouseRepository.findByIsDefaultTrue()
                        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Chưa có kho mặc định"));

        Inventory inventory = inventoryRepository.findByVariantIdAndWarehouseId(variant.getId(), warehouse.getId())
                .orElseGet(() -> Inventory.builder().variantId(variant.getId()).warehouseId(warehouse.getId()).build());

        int before = inventory.getQuantityOnHand();
        int after = before + request.quantity();
        inventory.setQuantityOnHand(after);

        try {
            inventory = inventoryRepository.save(inventory);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessException(ErrorCode.INVENTORY_CONFLICT);
        }

        inventoryTransactionRepository.save(InventoryTransaction.builder()
                .warehouseId(warehouse.getId())
                .variantId(variant.getId())
                .transactionType(InventoryTransactionType.IMPORT)
                .quantity(request.quantity())
                .quantityBefore(before)
                .quantityAfter(after)
                .referenceType("MANUAL_IMPORT")
                .note(request.note())
                .build());

        return InventoryResponse.from(inventory);
    }
}
