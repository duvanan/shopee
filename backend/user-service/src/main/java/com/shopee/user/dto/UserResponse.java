package com.shopee.user.dto;

import com.shopee.user.entity.User;

import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        List<String> roles
) {
    /** roles duoc truyen tu ngoai vao (doc bang native query rieng), User khong tu mang theo. */
    public static UserResponse from(User user, List<String> roles) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getPhone(), roles);
    }
}
