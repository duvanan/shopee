package com.shopee.product.controller;

import com.shopee.common.response.ApiResponse;
import com.shopee.common.response.PageData;
import com.shopee.product.dto.ProductRequest;
import com.shopee.product.dto.ProductResponse;
import com.shopee.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Cac endpoint ghi (POST/PUT/DELETE) khong con tu check quyen bang
 * @PreAuthorize - Gateway da chan theo permission (PRODUCT_CREATE/UPDATE/
 * DELETE) truoc khi request toi duoc day, xem docs/architecture/rbac.md.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<PageData<ProductResponse>> list(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageData<ProductResponse> response = productService.list(categoryId, search, page, size);
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(productService.get(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("PRODUCT_CREATED", "Tạo sản phẩm thành công", response));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.update(id, request);
        return ApiResponse.success("PRODUCT_UPDATED", "Cập nhật sản phẩm thành công", response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
