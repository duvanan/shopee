package com.shopee.product.dto;

import com.shopee.product.entity.Brand;

import java.time.Instant;
import java.util.UUID;

public record BrandResponse(
        UUID id,
        String code,
        String name,
        String slug,
        String logoUrl,
        String bannerUrl,
        String description,
        String websiteUrl,
        String countryCode,
        String status,
        Integer sortOrder,
        String seoTitle,
        String seoDescription,
        Instant createdAt,
        Instant updatedAt
) {
    public static BrandResponse from(Brand brand) {
        return new BrandResponse(
                brand.getId(), brand.getCode(), brand.getName(), brand.getSlug(),
                brand.getLogoUrl(), brand.getBannerUrl(), brand.getDescription(),
                brand.getWebsiteUrl(), brand.getCountryCode(), brand.getStatus().name(),
                brand.getSortOrder(), brand.getSeoTitle(), brand.getSeoDescription(),
                brand.getCreatedAt(), brand.getUpdatedAt()
        );
    }
}
