package com.shopee.product.dto;

import java.util.List;
import java.util.UUID;

public record AttributeSchemaFieldResponse(
        UUID attributeId,
        String code,
        String name,
        String label,
        String dataType,
        String inputType,
        String unit,
        boolean required,
        boolean variantAttribute,
        boolean filterable,
        boolean searchable,
        boolean comparable,
        boolean visible,
        boolean editable,
        String placeholder,
        String helpText,
        UUID defaultValueId,
        String defaultValueText,
        List<AttributeValueResponse> values
) {
}
