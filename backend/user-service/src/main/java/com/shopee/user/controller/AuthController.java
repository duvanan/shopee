package com.shopee.user.controller;

import com.shopee.common.response.ApiResponse;
import com.shopee.user.dto.AuthResponse;
import com.shopee.user.dto.ChangePasswordRequest;
import com.shopee.user.dto.ForgotPasswordRequest;
import com.shopee.user.dto.LoginRequest;
import com.shopee.user.dto.RegisterRequest;
import com.shopee.user.dto.ResetPasswordRequest;
import com.shopee.user.dto.SessionResponse;
import com.shopee.user.security.DeviceInfo;
import com.shopee.user.service.AuthService;
import com.shopee.user.service.AuthService.SessionResult;
import com.shopee.user.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * accessToken (JWT, song ngan) tra trong `data.accessToken` cua ApiResponse
 * nhu truoc. refreshToken (song dai, luu Redis theo session - xem
 * RefreshTokenService) khong bao gio xuat hien trong JSON, chi gan qua
 * Set-Cookie httpOnly de giam rui ro bi doc boi XSS.
 *
 * Toan bo orchestration (rotate/revoke token, ghi login history...) nam o
 * AuthService/PasswordResetService - controller chi convert HttpServletRequest
 * -> DeviceInfo va map ket qua service thanh ResponseEntity.
 */
@RestController
@RequestMapping("/api/users/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        SessionResult result = authService.register(request, DeviceInfo.from(httpRequest));
        return withSessionCookie(HttpStatus.CREATED, "REGISTER_SUCCESS", "Đăng ký thành công", result);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        SessionResult result = authService.login(request, DeviceInfo.from(httpRequest));
        return withSessionCookie(HttpStatus.OK, "LOGIN_SUCCESS", "Đăng nhập thành công", result);
    }

    /** Goi khi access token het han - client dinh kem cookie refreshToken, khong can Authorization header. */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletRequest httpRequest) {
        SessionResult result = authService.refresh(refreshToken, DeviceInfo.from(httpRequest));
        return withSessionCookie(HttpStatus.OK, "TOKEN_REFRESHED", "Làm mới token thành công", result);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletRequest httpRequest) {
        var cookie = authService.logout(refreshToken, DeviceInfo.from(httpRequest));
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    /** Dang xuat tat ca thiet bi - VD nghi ngo bi lo tai khoan. Can token hop le (X-User-Id do Gateway gan). */
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@RequestHeader("X-User-Id") UUID userId, HttpServletRequest httpRequest) {
        var cookie = authService.logoutAll(userId, DeviceInfo.from(httpRequest));
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    /** Danh sach thiet bi/phien dang dang nhap cua chinh minh. */
    @GetMapping("/sessions")
    public ApiResponse<List<SessionResponse>> listSessions(@RequestHeader("X-User-Id") UUID userId) {
        return ApiResponse.success(authService.listSessions(userId));
    }

    /** Thu hoi 1 thiet bi cu the (khong nhat thiet la thiet bi dang goi request nay). */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> revokeSession(@RequestHeader("X-User-Id") UUID userId, @PathVariable String sessionId,
                                               HttpServletRequest httpRequest) {
        authService.revokeSession(userId, sessionId, DeviceInfo.from(httpRequest));
        return ResponseEntity.noContent().build();
    }

    /** Doi mat khau khi dang dang nhap - khac /reset-password (khong can email/token). */
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@RequestHeader("X-User-Id") UUID userId,
                                             @Valid @RequestBody ChangePasswordRequest request,
                                             HttpServletRequest httpRequest) {
        authService.changePassword(userId, request.currentPassword(), request.newPassword(), DeviceInfo.from(httpRequest));
        return ApiResponse.success("PASSWORD_CHANGED", "Đổi mật khẩu thành công, vui lòng đăng nhập lại");
    }

    /** Luon tra ve message chung, khong cho biet email co ton tai hay khong (tranh do tai khoan). */
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        passwordResetService.requestReset(request.email(), DeviceInfo.from(httpRequest));
        return ApiResponse.success("RESET_EMAIL_SENT_IF_EXISTS",
                "Nếu email tồn tại, chúng tôi đã gửi liên kết đặt lại mật khẩu");
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest httpRequest) {
        passwordResetService.resetPassword(request.token(), request.newPassword(), DeviceInfo.from(httpRequest));
        return ApiResponse.success("PASSWORD_RESET", "Đặt lại mật khẩu thành công, vui lòng đăng nhập lại");
    }

    private ResponseEntity<ApiResponse<AuthResponse>> withSessionCookie(
            HttpStatus status, String code, String message, SessionResult result) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, result.cookie().toString())
                .body(ApiResponse.success(code, message, result.body()));
    }
}
