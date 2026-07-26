package com.shopee.product.dto;

import com.shopee.product.entity.Category;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String slug
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug());
    }
}
