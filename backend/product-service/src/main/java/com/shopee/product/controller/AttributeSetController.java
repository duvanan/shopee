package com.shopee.product.controller;

import com.shopee.common.response.ApiResponse;
import com.shopee.product.dto.*;
import com.shopee.product.service.AttributeSetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/attribute-sets")
public class AttributeSetController {

    private final AttributeSetService attributeSetService;

    public AttributeSetController(AttributeSetService attributeSetService) {
        this.attributeSetService = attributeSetService;
    }

    @GetMapping
    public ApiResponse<List<AttributeSetResponse>> list() {
        return ApiResponse.success(attributeSetService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<AttributeSetResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(attributeSetService.get(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AttributeSetResponse>> create(@Valid @RequestBody AttributeSetRequest request) {
        AttributeSetResponse response = attributeSetService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("ATTRIBUTE_SET_CREATED", "Đã tạo bộ thuộc tính", response));
    }

    @PutMapping("/{id}")
    public ApiResponse<AttributeSetResponse> update(@PathVariable UUID id, @Valid @RequestBody AttributeSetRequest request) {
        return ApiResponse.success("ATTRIBUTE_SET_UPDATED", "Đã cập nhật bộ thuộc tính", attributeSetService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        attributeSetService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/clone")
    public ResponseEntity<ApiResponse<AttributeSetResponse>> clone(@PathVariable UUID id, @Valid @RequestBody CloneAttributeSetRequest request) {
        AttributeSetResponse response = attributeSetService.clone(id, request.code(), request.name());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("ATTRIBUTE_SET_CLONED", "Đã nhân bản bộ thuộc tính", response));
    }

    @GetMapping("/{id}/schema")
    public ApiResponse<AttributeSchemaResponse> getSchema(@PathVariable UUID id) {
        return ApiResponse.success(attributeSetService.getSchema(id));
    }

    @PutMapping("/{id}/schema")
    public ApiResponse<AttributeSchemaResponse> setSchema(@PathVariable UUID id, @Valid @RequestBody SetAttributeSetSchemaRequest request) {
        return ApiResponse.success("ATTRIBUTE_SET_SCHEMA_UPDATED", "Đã lưu cấu hình bộ thuộc tính", attributeSetService.setSchema(id, request));
    }
}
