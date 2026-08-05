package com.shopee.product.controller;

import com.shopee.common.response.ApiResponse;
import com.shopee.product.dto.AttributeSchemaResponse;
import com.shopee.product.dto.ProductTypeRequest;
import com.shopee.product.dto.ProductTypeResponse;
import com.shopee.product.service.AttributeSetService;
import com.shopee.product.service.ProductTypeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/product-types")
public class ProductTypeController {

    private final ProductTypeService productTypeService;
    private final AttributeSetService attributeSetService;

    public ProductTypeController(ProductTypeService productTypeService, AttributeSetService attributeSetService) {
        this.productTypeService = productTypeService;
        this.attributeSetService = attributeSetService;
    }

    /** API schema dong (muc 15) - FE goi khi admin chon Product Type luc tao san pham de tu sinh form. */
    @GetMapping("/{id}/attribute-schema")
    public ApiResponse<AttributeSchemaResponse> getAttributeSchema(@PathVariable UUID id) {
        return ApiResponse.success(attributeSetService.getSchemaByProductType(id));
    }

    @GetMapping
    public ApiResponse<List<ProductTypeResponse>> list() {
        return ApiResponse.success(productTypeService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductTypeResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(productTypeService.get(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductTypeResponse>> create(@Valid @RequestBody ProductTypeRequest request) {
        ProductTypeResponse response = productTypeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("PRODUCT_TYPE_CREATED", "Đã tạo loại sản phẩm", response));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductTypeResponse> update(@PathVariable UUID id, @Valid @RequestBody ProductTypeRequest request) {
        return ApiResponse.success("PRODUCT_TYPE_UPDATED", "Đã cập nhật loại sản phẩm", productTypeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        productTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    public ApiResponse<Void> activate(@PathVariable UUID id) {
        productTypeService.activate(id);
        return ApiResponse.success("PRODUCT_TYPE_ACTIVATED", "Đã kích hoạt");
    }

    @PostMapping("/{id}/deactivate")
    public ApiResponse<Void> deactivate(@PathVariable UUID id) {
        productTypeService.deactivate(id);
        return ApiResponse.success("PRODUCT_TYPE_DEACTIVATED", "Đã ngừng hoạt động");
    }
}
