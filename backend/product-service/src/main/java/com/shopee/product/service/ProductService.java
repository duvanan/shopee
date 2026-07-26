package com.shopee.product.service;

import com.shopee.common.exception.BusinessException;
import com.shopee.common.exception.ErrorCode;
import com.shopee.common.response.PageData;
import com.shopee.product.dto.ProductRequest;
import com.shopee.product.dto.ProductResponse;
import com.shopee.product.entity.Category;
import com.shopee.product.entity.Product;
import com.shopee.product.repository.CategoryRepository;
import com.shopee.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Khong dua vao JPA relationship (@ManyToOne) de lay ten category - tu
 * batch-fetch Category rieng roi ghep tay vao ProductResponse. Chi 1
 * cau query CategoryRepository.findAllById() cho ca trang, khong phu
 * thuoc lazy loading/entity graph cua Hibernate.
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    private static final int MAX_PAGE_SIZE = 100;

    /** page: 1-based (quy uoc chung, xem docs/architecture/api-response-standard.md muc 14). */
    public PageData<ProductResponse> list(UUID categoryId, String search, int page, int size) {
        Pageable pageable = toPageable(page, size);

        boolean hasCategory = categoryId != null;
        boolean hasSearch = search != null && !search.isBlank();

        Page<Product> result;
        if (hasCategory && hasSearch) {
            result = productRepository.findByCategoryIdAndNameContainingIgnoreCase(categoryId, search, pageable);
        } else if (hasCategory) {
            result = productRepository.findByCategoryId(categoryId, pageable);
        } else if (hasSearch) {
            result = productRepository.findByNameContainingIgnoreCase(search, pageable);
        } else {
            result = productRepository.findAll(pageable);
        }

        Map<UUID, Category> categoriesById = fetchCategoriesFor(result.getContent());
        return PageData.from(result, product -> ProductResponse.from(product, categoriesById.get(product.getCategoryId())));
    }

    private Pageable toPageable(int page, int size) {
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public ProductResponse get(UUID id) {
        Product product = findOrThrow(id);
        return ProductResponse.from(product, fetchCategoryOrNull(product.getCategoryId()));
    }

    public ProductResponse create(ProductRequest request) {
        Category category = resolveCategory(request.categoryId());
        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .stockQuantity(request.stockQuantity())
                .imageUrl(request.imageUrl())
                .categoryId(request.categoryId())
                .build();
        return ProductResponse.from(productRepository.save(product), category);
    }

    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = findOrThrow(id);
        Category category = resolveCategory(request.categoryId());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setImageUrl(request.imageUrl());
        product.setCategoryId(request.categoryId());
        return ProductResponse.from(productRepository.save(product), category);
    }

    public void delete(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        productRepository.deleteById(id);
    }

    private Product findOrThrow(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    /** null neu khong truyen categoryId; loi 400 neu truyen nhung khong ton tai. */
    private Category resolveCategory(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private Category fetchCategoryOrNull(UUID categoryId) {
        return categoryId != null ? categoryRepository.findById(categoryId).orElse(null) : null;
    }

    private Map<UUID, Category> fetchCategoriesFor(Iterable<Product> products) {
        Set<UUID> categoryIds = new HashSet<>();
        for (Product product : products) {
            if (product.getCategoryId() != null) {
                categoryIds.add(product.getCategoryId());
            }
        }
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        return categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, category -> category));
    }
}
