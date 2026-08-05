package com.shopee.user.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Thay toan bo tap permission cua role bang danh sach nay (xem AdminRoleController.setPermissions). */
public record SetRolePermissionsRequest(@NotNull List<String> permissionCodes) {
}
