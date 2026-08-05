package com.shopee.product.dto;

import jakarta.validation.constraints.NotBlank;

public record BrandRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String slug,
        String logoUrl,
        String bannerUrl,
        String description,
        String websiteUrl,
        String countryCode,
        Integer sortOrder,
        String seoTitle,
        String seoDescription
) {
}
