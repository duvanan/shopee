package com.shopee.user.dto;

import com.shopee.user.entity.Role;

import java.util.UUID;

public record RoleResponse(UUID id, String code, String name, String description, boolean isSystem) {
    public static RoleResponse from(Role role) {
        return new RoleResponse(role.getId(), role.getCode(), role.getName(), role.getDescription(), role.isSystem());
    }
}
