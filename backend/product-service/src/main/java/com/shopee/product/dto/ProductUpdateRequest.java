package com.shopee.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/** Chi cap nhat thong tin chung + gia tri thuoc tinh mo ta - Product Type/Attribute Set/cau truc variant giu nguyen sau khi tao. */
public record ProductUpdateRequest(
        @NotBlank String name,
        @NotBlank String slug,
        UUID brandId,
        @NotEmpty List<UUID> categoryIds,
        UUID primaryCategoryId,
        String shortDescription,
        String description,
        String status,
        Boolean isFeatured,
        @Valid ProductSeoRequest seo,
        @Valid List<ProductAttributeValueInput> attributeValues
) {
    public boolean featured() {
        return Boolean.TRUE.equals(isFeatured);
    }
}
