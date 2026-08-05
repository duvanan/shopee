package com.shopee.user.dto;

import com.shopee.user.entity.Permission;

import java.util.UUID;

public record PermissionResponse(UUID id, String code, String name, String description, String module) {
    public static PermissionResponse from(Permission permission) {
        return new PermissionResponse(permission.getId(), permission.getCode(), permission.getName(),
                permission.getDescription(), permission.getModule());
    }
}
