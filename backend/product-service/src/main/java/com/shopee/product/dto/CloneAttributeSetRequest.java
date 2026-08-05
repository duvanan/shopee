package com.shopee.product.dto;

import jakarta.validation.constraints.NotBlank;

public record CloneAttributeSetRequest(
        @NotBlank String code,
        @NotBlank String name
) {
}
