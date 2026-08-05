package com.shopee.product.dto;

import com.shopee.product.entity.Product;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSummaryResponse(
        UUID id,
        String code,
        String name,
        String slug,
        String thumbnailUrl,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String brandName,
        String primaryCategoryName,
        String status,
        boolean isFeatured
) {
    public static ProductSummaryResponse from(Product product, String brandName, String primaryCategoryName) {
        return new ProductSummaryResponse(
                product.getId(), product.getCode(), product.getName(), product.getSlug(),
                product.getThumbnailUrl(), product.getMinPrice(), product.getMaxPrice(),
                brandName, primaryCategoryName, product.getStatus().name(), product.isFeatured()
        );
    }
}
