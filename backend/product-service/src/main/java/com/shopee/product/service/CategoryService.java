package com.shopee.product.service;

import com.shopee.common.exception.BusinessException;
import com.shopee.common.exception.ErrorCode;
import com.shopee.product.dto.CategoryRequest;
import com.shopee.product.dto.CategoryResponse;
import com.shopee.product.entity.Category;
import com.shopee.product.repository.CategoryRepository;
import com.shopee.product.repository.ProductCategoryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/** Category chi phuc vu dieu huong/SEO/marketing - KHONG quyet dinh Attribute Set (xem docs/architecture/product-type-attribute-set-dynamic-product-ui-ux-spec-flyway.md muc 42). */
@Service
public class CategoryService {

    private static final Sort SORT_ORDER_THEN_NAME = Sort.by("sortOrder", "name");

    private final CategoryRepository categoryRepository;
    private final ProductCategoryRepository productCategoryRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductCategoryRepository productCategoryRepository) {
        this.categoryRepository = categoryRepository;
        this.productCategoryRepository = productCategoryRepository;
    }

    public List<CategoryResponse> list() {
        return categoryRepository.findAll(SORT_ORDER_THEN_NAME).stream().map(CategoryResponse::from).toList();
    }

    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByCode(request.code())) {
            throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "Mã danh mục đã tồn tại");
        }
        if (categoryRepository.existsBySlug(request.slug())) {
            throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "Slug danh mục đã tồn tại");
        }

        int level = 0;
        String path = null;
        if (request.parentId() != null) {
            Category parent = findOrThrow(request.parentId());
            level = parent.getLevel() + 1;
            path = (parent.getPath() == null ? parent.getId().toString() : parent.getPath() + "/" + parent.getId());
        }

        Category category = Category.builder()
                .parentId(request.parentId())
                .code(request.code())
                .name(request.name())
                .slug(request.slug())
                .description(request.description())
                .imageUrl(request.imageUrl())
                .level(level)
                .path(path)
                .sortOrder(request.sortOrder() == null ? 0 : request.sortOrder())
                .seoTitle(request.seoTitle())
                .seoDescription(request.seoDescription())
                .build();
        return CategoryResponse.from(categoryRepository.save(category));
    }

    public CategoryResponse update(UUID id, CategoryRequest request) {
        Category category = findOrThrow(id);
        category.setName(request.name());
        category.setSlug(request.slug());
        category.setDescription(request.description());
        category.setImageUrl(request.imageUrl());
        category.setSortOrder(request.sortOrder() == null ? category.getSortOrder() : request.sortOrder());
        category.setSeoTitle(request.seoTitle());
        category.setSeoDescription(request.seoDescription());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    public void delete(UUID id) {
        if (productCategoryRepository.existsByCategoryId(id)) {
            throw new BusinessException(ErrorCode.CATEGORY_HAS_PRODUCTS);
        }
        if (!categoryRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        categoryRepository.deleteById(id);
    }

    private Category findOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }
}
