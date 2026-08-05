package com.shopee.user.dto;

import jakarta.validation.constraints.NotBlank;

/** Khong doi duoc "code" - la dinh danh bat bien cua role, dung trong JWT/permission check. */
public record RoleUpdateRequest(@NotBlank String name, String description) {
}
