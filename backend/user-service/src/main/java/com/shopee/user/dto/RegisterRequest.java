package com.shopee.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự") String password,
        @NotBlank String fullName,
        String phone
) {
}
