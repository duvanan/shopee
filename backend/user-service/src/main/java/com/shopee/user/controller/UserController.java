package com.shopee.user.controller;

import com.shopee.common.response.ApiResponse;
import com.shopee.user.dto.UserResponse;
import com.shopee.user.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@RequestHeader("X-User-Id") UUID userId) {
        return ApiResponse.success(userService.getProfile(userId));
    }
}
