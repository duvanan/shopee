package com.shopee.user.dto;

import com.shopee.user.entity.CustomerAddress;

import java.util.UUID;

public record AddressResponse(
        UUID id,
        String recipientName,
        String phoneNumber,
        String provinceCode,
        String provinceName,
        String districtCode,
        String districtName,
        String wardCode,
        String wardName,
        String addressLine,
        String addressType,
        String label,
        boolean isDefault
) {
    public static AddressResponse from(CustomerAddress address) {
        return new AddressResponse(
                address.getId(), address.getRecipientName(), address.getPhoneNumber(),
                address.getProvinceCode(), address.getProvinceName(),
                address.getDistrictCode(), address.getDistrictName(),
                address.getWardCode(), address.getWardName(),
                address.getAddressLine(), address.getAddressType().name(),
                address.getLabel(), address.isDefault());
    }
}
