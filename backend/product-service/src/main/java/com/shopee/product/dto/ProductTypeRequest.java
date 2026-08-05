package com.shopee.product.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record ProductTypeRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        UUID defaultAttributeSetId,
        String iconUrl,
        Integer sortOrder
) {
}
