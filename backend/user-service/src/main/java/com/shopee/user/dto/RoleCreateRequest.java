package com.shopee.user.dto;

import jakarta.validation.constraints.NotBlank;

public record RoleCreateRequest(@NotBlank String code, @NotBlank String name, String description) {
}
