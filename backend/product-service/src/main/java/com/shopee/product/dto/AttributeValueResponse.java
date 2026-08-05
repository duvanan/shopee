package com.shopee.product.dto;

import com.shopee.product.entity.AttributeValue;

import java.util.UUID;

public record AttributeValueResponse(
        UUID id,
        UUID attributeId,
        String code,
        String value,
        String displayValue,
        String colorCode,
        String imageUrl,
        Integer sortOrder,
        String status
) {
    public static AttributeValueResponse from(AttributeValue value) {
        return new AttributeValueResponse(
                value.getId(), value.getAttributeId(), value.getCode(), value.getValue(),
                value.getDisplayValue(), value.getColorCode(), value.getImageUrl(),
                value.getSortOrder(), value.getStatus().name()
        );
    }
}
