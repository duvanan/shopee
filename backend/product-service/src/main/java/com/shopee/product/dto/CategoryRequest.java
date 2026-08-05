package com.shopee.product.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CategoryRequest(
        UUID parentId,
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String slug,
        String description,
        String imageUrl,
        Integer sortOrder,
        String seoTitle,
        String seoDescription
) {
}
