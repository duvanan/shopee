package com.shopee.product.controller;

import com.shopee.common.exception.BusinessException;
import com.shopee.common.exception.ErrorCode;
import com.shopee.common.response.ApiResponse;
import com.shopee.product.dto.CategoryResponse;
import com.shopee.product.entity.Category;
import com.shopee.product.repository.CategoryRepository;
import com.shopee.product.repository.ProductRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryController(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    public record CategoryRequest(@NotBlank String name, String slug) {
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> list() {
        List<CategoryResponse> response = categoryRepository.findAll().stream()
                .map(CategoryResponse::from)
                .toList();
        return ApiResponse.success(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CategoryRequest request) {
        Category category = Category.builder().name(request.name()).slug(request.slug()).build();
        CategoryResponse response = CategoryResponse.from(categoryRepository.save(category));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("CATEGORY_CREATED", "Tạo danh mục thành công", response));
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> update(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        category.setName(request.name());
        category.setSlug(request.slug());
        CategoryResponse response = CategoryResponse.from(categoryRepository.save(category));
        return ApiResponse.success("CATEGORY_UPDATED", "Cập nhật danh mục thành công", response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (productRepository.existsByCategoryId(id)) {
            throw new BusinessException(ErrorCode.CATEGORY_HAS_PRODUCTS);
        }
        categoryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
