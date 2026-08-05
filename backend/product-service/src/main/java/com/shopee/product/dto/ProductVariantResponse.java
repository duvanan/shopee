package com.shopee.product.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductVariantResponse(
        UUID id,
        String sku,
        String barcode,
        String name,
        boolean isDefault,
        String status,
        BigDecimal listPrice,
        BigDecimal salePrice,
        List<VariantAttributeValueResponse> attributeValues,
        BigDecimal weight,
        String weightUnit,
        BigDecimal length,
        BigDecimal width,
        BigDecimal height,
        String dimensionUnit,
        Integer quantityAvailable,
        String imageUrl
) {
}
