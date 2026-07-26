package com.shopee.user.controller;

import com.shopee.common.exception.BusinessException;
import com.shopee.common.exception.ErrorCode;
import com.shopee.common.response.ApiResponse;
import com.shopee.user.dto.AuthResponse;
import com.shopee.user.dto.LoginRequest;
import com.shopee.user.dto.RegisterRequest;
import com.shopee.user.security.RefreshTokenService;
import com.shopee.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * accessToken (JWT, song ngan) tra trong `data.accessToken` cua ApiResponse
 * nhu truoc. refreshToken (song dai, luu Redis - xem RefreshTokenService)
 * khong bao gio xuat hien trong JSON, chi gan qua Set-Cookie httpOnly de
 * giam rui ro bi doc boi XSS.
 */
@RestController
@RequestMapping("/api/users/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthService authService, RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse body = authService.register(request);
        return withRefreshCookie(HttpStatus.CREATED, ApiResponse.success("REGISTER_SUCCESS", "Đăng ký thành công", body), body);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse body = authService.login(request);
        return withRefreshCookie(HttpStatus.OK, ApiResponse.success("LOGIN_SUCCESS", "Đăng nhập thành công", body), body);
    }

    /** Goi khi access token het han - client dinh kem cookie refreshToken, khong can Authorization header. */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_MISSING);
        }
        UUID userId = refreshTokenService.resolveAndRevoke(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));

        AuthResponse body = authService.refreshAccessToken(userId);
        return withRefreshCookie(HttpStatus.OK, ApiResponse.success("TOKEN_REFRESHED", "Làm mới token thành công", body), body);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken) {
        if (refreshToken != null) {
            refreshTokenService.revoke(refreshToken);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenService.expiredCookie().toString())
                .build();
    }

    private ResponseEntity<ApiResponse<AuthResponse>> withRefreshCookie(
            HttpStatus status, ApiResponse<AuthResponse> apiResponse, AuthResponse body) {
        ResponseCookie cookie = refreshTokenService.issueCookie(body.user().id());
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(apiResponse);
    }
}
