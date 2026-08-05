package com.shopee.product.dto;

import com.shopee.product.entity.ProductType;

import java.util.UUID;

public record ProductTypeResponse(
        UUID id,
        String code,
        String name,
        String description,
        UUID defaultAttributeSetId,
        String defaultAttributeSetName,
        String iconUrl,
        String status,
        Integer sortOrder,
        long productCount
) {
    public static ProductTypeResponse from(ProductType type, String defaultAttributeSetName, long productCount) {
        return new ProductTypeResponse(
                type.getId(), type.getCode(), type.getName(), type.getDescription(),
                type.getDefaultAttributeSetId(), defaultAttributeSetName, type.getIconUrl(),
                type.getStatus().name(), type.getSortOrder(), productCount
        );
    }
}
