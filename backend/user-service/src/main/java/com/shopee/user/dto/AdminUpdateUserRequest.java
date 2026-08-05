package com.shopee.user.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminUpdateUserRequest(@NotBlank String fullName, String phone) {
}
