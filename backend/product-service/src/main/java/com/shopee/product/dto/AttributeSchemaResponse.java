package com.shopee.product.dto;

import java.util.List;
import java.util.UUID;

public record AttributeSchemaResponse(
        UUID productTypeId,
        String productTypeCode,
        String productTypeName,
        UUID attributeSetId,
        String attributeSetCode,
        String attributeSetName,
        int attributeSetVersion,
        List<AttributeSchemaGroupResponse> groups
) {
}
