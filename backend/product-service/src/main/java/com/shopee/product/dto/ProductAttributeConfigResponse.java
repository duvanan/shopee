package com.shopee.product.dto;

import java.util.List;
import java.util.UUID;

public record ProductAttributeConfigResponse(
        UUID attributeId,
        String code,
        String name,
        String inputType,
        boolean isVariantAttribute,
        boolean isVisible,
        List<AttributeValueResponse> values,
        String valueText
) {
}
