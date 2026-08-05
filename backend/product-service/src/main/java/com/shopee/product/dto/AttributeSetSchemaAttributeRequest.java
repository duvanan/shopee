package com.shopee.product.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Boolean dung wrapper (khong phai primitive) - field thieu trong JSON se null thay vi vo Jackson (xem ProductAttributeConfigRequest). */
public record AttributeSetSchemaAttributeRequest(
        @NotNull UUID attributeId,
        String labelOverride,
        String helpText,
        String placeholder,
        Boolean isRequired,
        Boolean isVariantAttribute,
        Boolean isFilterable,
        Boolean isSearchable,
        Boolean isComparable,
        Boolean isVisible,
        Boolean isEditable,
        UUID defaultValueId,
        String defaultValueText,
        Integer sortOrder
) {
    public boolean required() { return Boolean.TRUE.equals(isRequired); }
    public boolean variantAttribute() { return Boolean.TRUE.equals(isVariantAttribute); }
    public boolean filterable() { return Boolean.TRUE.equals(isFilterable); }
    public boolean searchable() { return Boolean.TRUE.equals(isSearchable); }
    public boolean comparable() { return Boolean.TRUE.equals(isComparable); }
    public boolean visible() { return isVisible == null || isVisible; }
    public boolean editable() { return isEditable == null || isEditable; }
}
