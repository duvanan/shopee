package com.shopee.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductAttributeRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotNull String dataType,
        @NotNull String inputType,
        String unit,
        String attributeGroup,
        Boolean isFilterable,
        Boolean isSearchable,
        Boolean isComparable,
        Boolean allowVariant,
        Integer sortOrder
) {
}
