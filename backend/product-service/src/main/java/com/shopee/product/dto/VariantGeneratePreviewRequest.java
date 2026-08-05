package com.shopee.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record VariantGeneratePreviewRequest(
        @NotEmpty @Valid List<AttributeValueGroup> attributeGroups
) {
}
