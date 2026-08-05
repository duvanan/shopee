package com.shopee.user.service;

import com.shopee.common.exception.BusinessException;
import com.shopee.common.exception.ErrorCode;
import com.shopee.user.dto.AddressRequest;
import com.shopee.user.dto.AddressResponse;
import com.shopee.user.entity.AddressType;
import com.shopee.user.entity.CustomerAddress;
import com.shopee.user.repository.CustomerAddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Moi user chi co toi da 1 dia chi mac dinh (xem
 * docs/architecture/user-auth-role-permission-redis-refresh-token-design-uuid.md
 * muc 10.6) - dat mac dinh 1 cai thi unset toan bo cai con lai cua chinh user do.
 */
@Service
public class CustomerAddressService {

    private final CustomerAddressRepository addressRepository;

    public CustomerAddressService(CustomerAddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(UUID userId) {
        return addressRepository.findByUserId(userId).stream().map(AddressResponse::from).toList();
    }

    @Transactional
    public AddressResponse create(UUID userId, AddressRequest request) {
        boolean isFirstAddress = addressRepository.findByUserId(userId).isEmpty();
        boolean shouldBeDefault = request.isDefault() || isFirstAddress;

        if (shouldBeDefault) {
            clearExistingDefault(userId);
        }

        CustomerAddress address = CustomerAddress.builder()
                .userId(userId)
                .recipientName(request.recipientName())
                .phoneNumber(request.phoneNumber())
                .provinceCode(request.provinceCode())
                .provinceName(request.provinceName())
                .districtCode(request.districtCode())
                .districtName(request.districtName())
                .wardCode(request.wardCode())
                .wardName(request.wardName())
                .addressLine(request.addressLine())
                .addressType(parseAddressType(request.addressType()))
                .label(request.label())
                .isDefault(shouldBeDefault)
                .build();

        return AddressResponse.from(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse update(UUID userId, UUID addressId, AddressRequest request) {
        CustomerAddress address = findOwnedOrThrow(userId, addressId);

        if (request.isDefault() && !address.isDefault()) {
            clearExistingDefault(userId);
            address.setDefault(true);
        }

        address.setRecipientName(request.recipientName());
        address.setPhoneNumber(request.phoneNumber());
        address.setProvinceCode(request.provinceCode());
        address.setProvinceName(request.provinceName());
        address.setDistrictCode(request.districtCode());
        address.setDistrictName(request.districtName());
        address.setWardCode(request.wardCode());
        address.setWardName(request.wardName());
        address.setAddressLine(request.addressLine());
        address.setAddressType(parseAddressType(request.addressType()));
        address.setLabel(request.label());

        return AddressResponse.from(addressRepository.save(address));
    }

    @Transactional
    public void delete(UUID userId, UUID addressId) {
        CustomerAddress address = findOwnedOrThrow(userId, addressId);
        boolean wasDefault = address.isDefault();
        addressRepository.delete(address);

        if (wasDefault) {
            addressRepository.findByUserId(userId).stream().findFirst().ifPresent(next -> {
                next.setDefault(true);
                addressRepository.save(next);
            });
        }
    }

    @Transactional
    public AddressResponse setDefault(UUID userId, UUID addressId) {
        CustomerAddress address = findOwnedOrThrow(userId, addressId);
        if (!address.isDefault()) {
            clearExistingDefault(userId);
            address.setDefault(true);
            address = addressRepository.save(address);
        }
        return AddressResponse.from(address);
    }

    private void clearExistingDefault(UUID userId) {
        addressRepository.findDefaultByUserId(userId).forEach(existing -> {
            existing.setDefault(false);
            addressRepository.save(existing);
        });
    }

    private CustomerAddress findOwnedOrThrow(UUID userId, UUID addressId) {
        CustomerAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy địa chỉ"));
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy địa chỉ");
        }
        return address;
    }

    private AddressType parseAddressType(String value) {
        if (value == null || value.isBlank()) {
            return AddressType.HOME;
        }
        try {
            return AddressType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "addressType không hợp lệ");
        }
    }
}
