package com.shopee.product.dto;

import jakarta.validation.constraints.NotBlank;

public record AttributeSetRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description
) {
}
