package com.shopee.product.controller;

import com.shopee.common.response.ApiResponse;
import com.shopee.product.dto.BrandRequest;
import com.shopee.product.dto.BrandResponse;
import com.shopee.product.service.BrandService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/brands")
public class BrandController {

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @GetMapping
    public ApiResponse<List<BrandResponse>> list() {
        return ApiResponse.success(brandService.list());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BrandResponse>> create(@Valid @RequestBody BrandRequest request) {
        BrandResponse response = brandService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("BRAND_CREATED", "Đã tạo thương hiệu", response));
    }

    @PutMapping("/{id}")
    public ApiResponse<BrandResponse> update(@PathVariable UUID id, @Valid @RequestBody BrandRequest request) {
        return ApiResponse.success("BRAND_UPDATED", "Đã cập nhật thương hiệu", brandService.update(id, request));
    }
}
