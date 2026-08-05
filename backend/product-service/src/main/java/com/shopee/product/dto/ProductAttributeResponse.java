package com.shopee.product.dto;

import com.shopee.product.entity.ProductAttribute;

import java.util.UUID;

public record ProductAttributeResponse(
        UUID id,
        String code,
        String name,
        String dataType,
        String inputType,
        String unit,
        String attributeGroup,
        boolean isFilterable,
        boolean isSearchable,
        boolean isComparable,
        boolean allowVariant,
        String status,
        Integer sortOrder
) {
    public static ProductAttributeResponse from(ProductAttribute attribute) {
        return new ProductAttributeResponse(
                attribute.getId(), attribute.getCode(), attribute.getName(),
                attribute.getDataType().name(), attribute.getInputType().name(),
                attribute.getUnit(), attribute.getAttributeGroup(),
                attribute.isFilterable(), attribute.isSearchable(), attribute.isComparable(),
                attribute.isAllowVariant(), attribute.getStatus().name(), attribute.getSortOrder()
        );
    }
}
