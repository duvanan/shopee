package com.shopee.product.controller;

import com.shopee.common.response.ApiResponse;
import com.shopee.product.dto.CategoryRequest;
import com.shopee.product.dto.CategoryResponse;
import com.shopee.product.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** GET public de FE xay filter danh muc; POST/PUT/DELETE can permission CATEGORY_MANAGE. */
@RestController
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/api/v1/public/categories")
    public ApiResponse<List<CategoryResponse>> listPublic() {
        return ApiResponse.success(categoryService.list());
    }

    @GetMapping("/api/v1/admin/categories")
    public ApiResponse<List<CategoryResponse>> listAdmin() {
        return ApiResponse.success(categoryService.list());
    }

    @PostMapping("/api/v1/admin/categories")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("CATEGORY_CREATED", "Đã tạo danh mục", response));
    }

    @PutMapping("/api/v1/admin/categories/{id}")
    public ApiResponse<CategoryResponse> update(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success("CATEGORY_UPDATED", "Đã cập nhật danh mục", categoryService.update(id, request));
    }

    @DeleteMapping("/api/v1/admin/categories/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
