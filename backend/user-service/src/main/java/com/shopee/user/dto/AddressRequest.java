package com.shopee.user.dto;

import jakarta.validation.constraints.NotBlank;

public record AddressRequest(
        @NotBlank String recipientName,
        @NotBlank String phoneNumber,
        @NotBlank String provinceCode,
        @NotBlank String provinceName,
        @NotBlank String districtCode,
        @NotBlank String districtName,
        @NotBlank String wardCode,
        @NotBlank String wardName,
        @NotBlank String addressLine,
        String addressType,
        String label,
        boolean isDefault
) {
}
