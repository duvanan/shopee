package com.shopee.user.dto;

import com.shopee.user.entity.User;

import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        List<String> roles,
        List<String> permissions
) {
    /**
     * roles/permissions duoc truyen tu ngoai vao (doc bang native query rieng,
     * xem RoleRepository), User khong tu mang theo. Frontend can permissions
     * o day (khong chi trong JWT) de quyet dinh hien thi UI (VD an/hien muc
     * trong sidebar /admin) - xem stores/auth.ts ben frontend.
     */
    public static UserResponse from(User user, List<String> roles, List<String> permissions) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getPhone(), roles, permissions);
    }
}
