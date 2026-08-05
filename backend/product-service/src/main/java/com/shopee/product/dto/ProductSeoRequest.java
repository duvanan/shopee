package com.shopee.product.dto;

public record ProductSeoRequest(
        String title,
        String description,
        String keywords,
        String canonicalUrl
) {
}
