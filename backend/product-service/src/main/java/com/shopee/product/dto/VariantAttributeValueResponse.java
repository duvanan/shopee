package com.shopee.product.dto;

import java.util.UUID;

public record VariantAttributeValueResponse(
        UUID attributeId,
        String attributeCode,
        String attributeName,
        UUID valueId,
        String value,
        String displayValue,
        String colorCode
) {
}
