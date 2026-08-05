package com.shopee.user.controller;

import com.shopee.common.response.ApiResponse;
import com.shopee.user.dto.AddressRequest;
import com.shopee.user.dto.AddressResponse;
import com.shopee.user.service.CustomerAddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Tu phuc vu - moi user chi thao tac tren dia chi cua chinh minh (X-User-Id do Gateway gan). */
@RestController
@RequestMapping("/api/users/addresses")
public class CustomerAddressController {

    private final CustomerAddressService addressService;

    public CustomerAddressController(CustomerAddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ApiResponse<List<AddressResponse>> list(@RequestHeader("X-User-Id") UUID userId) {
        return ApiResponse.success(addressService.list(userId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> create(
            @RequestHeader("X-User-Id") UUID userId, @Valid @RequestBody AddressRequest request) {
        AddressResponse response = addressService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("ADDRESS_CREATED", "Đã thêm địa chỉ", response));
    }

    @PutMapping("/{id}")
    public ApiResponse<AddressResponse> update(
            @RequestHeader("X-User-Id") UUID userId, @PathVariable UUID id, @Valid @RequestBody AddressRequest request) {
        return ApiResponse.success("ADDRESS_UPDATED", "Đã cập nhật địa chỉ", addressService.update(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID id) {
        addressService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/set-default")
    public ApiResponse<AddressResponse> setDefault(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID id) {
        return ApiResponse.success("ADDRESS_DEFAULT_SET", "Đã đặt làm địa chỉ mặc định", addressService.setDefault(userId, id));
    }
}
