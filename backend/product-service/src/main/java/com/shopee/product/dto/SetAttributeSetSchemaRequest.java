package com.shopee.product.dto;

import jakarta.validation.Valid;

import java.util.List;

public record SetAttributeSetSchemaRequest(
        @Valid List<AttributeSetSchemaGroupRequest> groups
) {
}
