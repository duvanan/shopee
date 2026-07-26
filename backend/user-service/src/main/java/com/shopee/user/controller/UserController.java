package com.shopee.user.controller;

import com.shopee.common.exception.BusinessException;
import com.shopee.common.exception.ErrorCode;
import com.shopee.common.response.ApiResponse;
import com.shopee.user.dto.UserResponse;
import com.shopee.user.entity.User;
import com.shopee.user.repository.RoleRepository;
import com.shopee.user.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserController(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@RequestHeader("X-User-Id") UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        UserResponse response = UserResponse.from(user, roleRepository.findRoleCodesByUserId(userId));
        return ApiResponse.success(response);
    }
}
