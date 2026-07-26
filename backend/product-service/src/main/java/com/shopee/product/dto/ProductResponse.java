package com.shopee.product.dto;

import com.shopee.product.entity.Category;
import com.shopee.product.entity.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        String imageUrl,
        UUID categoryId,
        String categoryName,
        Instant createdAt,
        Instant updatedAt
) {
    /** category co the null neu san pham chua gan danh muc, hoac categoryId tro toi 1 danh muc da bi xoa. */
    public static ProductResponse from(Product product, Category category) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getImageUrl(),
                product.getCategoryId(),
                category != null ? category.getName() : null,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
