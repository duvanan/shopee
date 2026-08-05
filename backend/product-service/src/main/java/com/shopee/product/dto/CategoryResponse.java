package com.shopee.product.dto;

import com.shopee.product.entity.Category;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        UUID parentId,
        String code,
        String name,
        String slug,
        String description,
        String imageUrl,
        Integer level,
        String path,
        Integer sortOrder,
        String status
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(), category.getParentId(), category.getCode(), category.getName(),
                category.getSlug(), category.getDescription(), category.getImageUrl(),
                category.getLevel(), category.getPath(), category.getSortOrder(),
                category.getStatus().name()
        );
    }
}
