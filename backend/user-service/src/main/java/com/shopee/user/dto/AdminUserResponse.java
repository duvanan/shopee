package com.shopee.user.dto;

import com.shopee.user.entity.User;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        String status,
        List<String> roles,
        Instant createdAt,
        Instant lastLoginAt
) {
    public static AdminUserResponse from(User user, List<String> roles) {
        return new AdminUserResponse(
                user.getId(), user.getEmail(), user.getFullName(), user.getPhone(),
                user.getStatus().name(), roles, user.getCreatedAt(), user.getLastLoginAt());
    }
}
