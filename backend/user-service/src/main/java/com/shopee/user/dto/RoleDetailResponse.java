package com.shopee.user.dto;

import com.shopee.user.entity.Role;

import java.util.List;
import java.util.UUID;

public record RoleDetailResponse(
        UUID id, String code, String name, String description, boolean isSystem, List<String> permissionCodes
) {
    public static RoleDetailResponse from(Role role, List<String> permissionCodes) {
        return new RoleDetailResponse(role.getId(), role.getCode(), role.getName(), role.getDescription(),
                role.isSystem(), permissionCodes);
    }
}
