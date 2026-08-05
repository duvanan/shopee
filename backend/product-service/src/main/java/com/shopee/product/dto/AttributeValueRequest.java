package com.shopee.product.dto;

import jakarta.validation.constraints.NotBlank;

public record AttributeValueRequest(
        @NotBlank String code,
        @NotBlank String value,
        String displayValue,
        String colorCode,
        String imageUrl,
        Integer sortOrder
) {
}
