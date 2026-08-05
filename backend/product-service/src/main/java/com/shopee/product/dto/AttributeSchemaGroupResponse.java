package com.shopee.product.dto;

import java.util.List;
import java.util.UUID;

public record AttributeSchemaGroupResponse(
        UUID id,
        String code,
        String name,
        Integer sortOrder,
        boolean collapsible,
        boolean defaultExpanded,
        List<AttributeSchemaFieldResponse> attributes
) {
}
