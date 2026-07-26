package com.shopee.user.service;

import com.shopee.user.dto.AuthResponse;
import com.shopee.user.dto.LoginRequest;
import com.shopee.user.dto.RegisterRequest;
import com.shopee.user.dto.UserResponse;
import com.shopee.user.entity.Role;
import com.shopee.user.entity.User;
import com.shopee.user.repository.RoleRepository;
import com.shopee.user.repository.UserRepository;
import com.shopee.user.security.JwtService;
import com.shopee.common.exception.BusinessException;
import com.shopee.common.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private static final String DEFAULT_ROLE_CODE = "CUSTOMER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                        PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        Role customerRole = roleRepository.findByCode(DEFAULT_ROLE_CODE)
                .orElseThrow(() -> new IllegalStateException(
                        "Role " + DEFAULT_ROLE_CODE + " chua duoc seed trong bang roles"));

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .phone(request.phone())
                .build();

        User saved = userRepository.save(user);
        roleRepository.assignRoleToUser(saved.getId(), customerRole.getId());

        List<String> roleCodes = List.of(customerRole.getCode());
        List<String> permissionCodes = roleRepository.findPermissionCodesByUserId(saved.getId());

        String token = jwtService.generateToken(saved, roleCodes, permissionCodes);
        return AuthResponse.of(token, UserResponse.from(saved, roleCodes));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        List<String> roleCodes = roleRepository.findRoleCodesByUserId(user.getId());
        List<String> permissionCodes = roleRepository.findPermissionCodesByUserId(user.getId());

        String token = jwtService.generateToken(user, roleCodes, permissionCodes);
        return AuthResponse.of(token, UserResponse.from(user, roleCodes));
    }

    /** Cap access token moi tu userId da duoc RefreshTokenService xac thuc qua refresh token cookie. */
    @Transactional(readOnly = true)
    public AuthResponse refreshAccessToken(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Người dùng không tồn tại"));

        List<String> roleCodes = roleRepository.findRoleCodesByUserId(user.getId());
        List<String> permissionCodes = roleRepository.findPermissionCodesByUserId(user.getId());

        String token = jwtService.generateToken(user, roleCodes, permissionCodes);
        return AuthResponse.of(token, UserResponse.from(user, roleCodes));
    }
}
