package com.shopee.product.dto;

import java.util.List;

public record VariantCombinationResponse(
        String signature,
        String suggestedName,
        List<VariantAttributeValueResponse> attributeValues
) {
}
