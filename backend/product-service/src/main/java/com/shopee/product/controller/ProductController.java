package com.shopee.product.controller;

import com.shopee.common.response.ApiResponse;
import com.shopee.common.response.PageData;
import com.shopee.product.dto.*;
import com.shopee.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoint ghi (POST/PUT/DELETE) khong tu check quyen bang @PreAuthorize -
 * Gateway da chan theo permission (xem docs/features/03-dynamic-product-catalog.md
 * muc 8 va api-gateway.yml).
 */
@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/api/v1/public/products")
    public ApiResponse<PageData<ProductSummaryResponse>> listPublic(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(productService.listPublic(categoryId, search, page, size));
    }

    @GetMapping("/api/v1/public/products/{slug}")
    public ApiResponse<ProductDetailResponse> getBySlug(@PathVariable String slug) {
        return ApiResponse.success(productService.getBySlug(slug));
    }

    @GetMapping("/api/v1/admin/products")
    public ApiResponse<PageData<ProductSummaryResponse>> listAdmin(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(productService.listAdmin(search, page, size));
    }

    @GetMapping("/api/v1/admin/products/{id}")
    public ApiResponse<ProductDetailResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(productService.getById(id));
    }

    @PostMapping("/api/v1/admin/products")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> create(
            @RequestHeader("X-User-Id") UUID actorId, @Valid @RequestBody ProductCreateRequest request) {
        ProductDetailResponse response = productService.create(actorId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("PRODUCT_CREATED", "Tạo sản phẩm thành công", response));
    }

    @PutMapping("/api/v1/admin/products/{id}")
    public ApiResponse<ProductDetailResponse> update(@PathVariable UUID id, @Valid @RequestBody ProductUpdateRequest request) {
        return ApiResponse.success("PRODUCT_UPDATED", "Cập nhật sản phẩm thành công", productService.update(id, request));
    }

    @DeleteMapping("/api/v1/admin/products/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/admin/products/variants/generate-preview")
    public ApiResponse<List<VariantCombinationResponse>> generatePreview(@Valid @RequestBody VariantGeneratePreviewRequest request) {
        return ApiResponse.success(productService.generatePreview(request));
    }
}
