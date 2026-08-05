package com.shopee.product.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductDetailResponse(
        UUID id,
        String code,
        String name,
        String slug,
        String structureType,
        UUID productTypeId,
        String productTypeName,
        UUID attributeSetId,
        String attributeSetName,
        Integer attributeSetVersion,
        UUID brandId,
        String brandName,
        List<CategoryResponse> categories,
        String shortDescription,
        String description,
        String status,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String thumbnailUrl,
        String seoTitle,
        String seoDescription,
        String seoKeywords,
        String canonicalUrl,
        boolean isFeatured,
        List<String> imageUrls,
        List<ProductAttributeConfigResponse> attributeConfigs,
        List<ProductVariantResponse> variants,
        Instant createdAt,
        Instant updatedAt
) {
}
