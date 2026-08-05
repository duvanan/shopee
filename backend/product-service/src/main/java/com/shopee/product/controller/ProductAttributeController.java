package com.shopee.product.controller;

import com.shopee.common.response.ApiResponse;
import com.shopee.product.dto.AttributeValueRequest;
import com.shopee.product.dto.AttributeValueResponse;
import com.shopee.product.dto.ProductAttributeRequest;
import com.shopee.product.dto.ProductAttributeResponse;
import com.shopee.product.service.ProductAttributeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/product-attributes")
public class ProductAttributeController {

    private final ProductAttributeService attributeService;

    public ProductAttributeController(ProductAttributeService attributeService) {
        this.attributeService = attributeService;
    }

    @GetMapping
    public ApiResponse<List<ProductAttributeResponse>> list() {
        return ApiResponse.success(attributeService.list());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductAttributeResponse>> create(@Valid @RequestBody ProductAttributeRequest request) {
        ProductAttributeResponse response = attributeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("ATTRIBUTE_CREATED", "Đã tạo thuộc tính", response));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductAttributeResponse> update(@PathVariable UUID id, @Valid @RequestBody ProductAttributeRequest request) {
        return ApiResponse.success("ATTRIBUTE_UPDATED", "Đã cập nhật thuộc tính", attributeService.update(id, request));
    }

    @GetMapping("/{id}/values")
    public ApiResponse<List<AttributeValueResponse>> listValues(@PathVariable UUID id) {
        return ApiResponse.success(attributeService.listValues(id));
    }

    @PostMapping("/{id}/values")
    public ResponseEntity<ApiResponse<AttributeValueResponse>> createValue(
            @PathVariable UUID id, @Valid @RequestBody AttributeValueRequest request) {
        AttributeValueResponse response = attributeService.createValue(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("ATTRIBUTE_VALUE_CREATED", "Đã tạo giá trị thuộc tính", response));
    }

    @PutMapping("/{id}/values/{valueId}")
    public ApiResponse<AttributeValueResponse> updateValue(
            @PathVariable UUID id, @PathVariable UUID valueId, @Valid @RequestBody AttributeValueRequest request) {
        return ApiResponse.success("ATTRIBUTE_VALUE_UPDATED", "Đã cập nhật giá trị thuộc tính",
                attributeService.updateValue(id, valueId, request));
    }
}
