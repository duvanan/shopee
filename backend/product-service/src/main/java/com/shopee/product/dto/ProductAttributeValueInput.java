package com.shopee.product.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Gia tri 1 thuoc tinh mo ta (khong phai variant) nhap cho san pham - chi dung 1 field value phu hop voi data_type cua attribute. */
public record ProductAttributeValueInput(
        @NotNull UUID attributeId,
        UUID attributeValueId,
        String valueText,
        BigDecimal valueNumber,
        Boolean valueBoolean,
        LocalDate valueDate
) {
}
