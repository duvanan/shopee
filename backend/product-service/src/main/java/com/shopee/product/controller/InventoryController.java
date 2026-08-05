package com.shopee.product.controller;

import com.shopee.common.response.ApiResponse;
import com.shopee.product.dto.InventoryImportRequest;
import com.shopee.product.dto.InventoryResponse;
import com.shopee.product.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/api/v1/admin/products/variants/{variantId}/inventory/import")
    public ApiResponse<InventoryResponse> importStock(@PathVariable UUID variantId, @Valid @RequestBody InventoryImportRequest request) {
        return ApiResponse.success("INVENTORY_IMPORTED", "Đã nhập kho", inventoryService.importStock(variantId, request));
    }
}
