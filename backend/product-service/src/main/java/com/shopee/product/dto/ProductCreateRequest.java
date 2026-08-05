package com.shopee.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ProductCreateRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String slug,
        @NotNull UUID productTypeId,
        String structureType,
        UUID brandId,
        @NotEmpty List<UUID> categoryIds,
        UUID primaryCategoryId,
        String shortDescription,
        String description,
        String status,
        Boolean isFeatured,
        @Valid ProductSeoRequest seo,
        @Valid List<ProductAttributeValueInput> attributeValues,
        @NotEmpty @Valid List<ProductVariantRequest> variants
) {
    public boolean featured() {
        return Boolean.TRUE.equals(isFeatured);
    }

    public String structureTypeOrDefault() {
        return structureType == null || structureType.isBlank() ? "SIMPLE" : structureType;
    }
}
