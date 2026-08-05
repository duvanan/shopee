package com.shopee.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record AttributeSetSchemaGroupRequest(
        UUID id,
        @NotBlank String code,
        @NotBlank String name,
        String description,
        Boolean isCollapsible,
        Boolean isDefaultExpanded,
        Integer sortOrder,
        @Valid List<AttributeSetSchemaAttributeRequest> attributes
) {
    public boolean collapsible() { return isCollapsible == null || isCollapsible; }
    public boolean defaultExpanded() { return isDefaultExpanded == null || isDefaultExpanded; }
}
