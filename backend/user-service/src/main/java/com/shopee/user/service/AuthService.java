package com.shopee.user.service;

import com.shopee.user.dto.AuthResponse;
import com.shopee.user.dto.LoginRequest;
import com.shopee.user.dto.RegisterRequest;
import com.shopee.user.dto.SessionResponse;
import com.shopee.user.dto.UserResponse;
import com.shopee.user.entity.LoginAction;
import com.shopee.user.entity.LoginStatus;
import com.shopee.user.entity.Role;
import com.shopee.user.entity.User;
import com.shopee.user.entity.UserStatus;
import com.shopee.user.repository.RoleRepository;
import com.shopee.user.repository.UserRepository;
import com.shopee.user.security.DeviceInfo;
import com.shopee.user.security.JwtService;
import com.shopee.user.security.RefreshTokenService;
import com.shopee.common.exception.BusinessException;
import com.shopee.common.exception.ErrorCode;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private static final String DEFAULT_ROLE_CODE = "CUSTOMER";
    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginHistoryService loginHistoryService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                        PasswordEncoder passwordEncoder, JwtService jwtService,
                        LoginHistoryService loginHistoryService, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginHistoryService = loginHistoryService;
        this.refreshTokenService = refreshTokenService;
    }

    /** Ket qua cac luong tao/lam moi session - body tra ve JSON, cookie gan qua Set-Cookie. */
    public record SessionResult(AuthResponse body, ResponseCookie cookie) {
    }

    @Transactional
    public SessionResult register(RegisterRequest request, DeviceInfo deviceInfo) {
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
        AuthResponse body = AuthResponse.of(token, UserResponse.from(saved, roleCodes, permissionCodes));
        ResponseCookie cookie = refreshTokenService.issueCookie(saved.getId(), deviceInfo);
        return new SessionResult(body, cookie);
    }

    @Transactional
    public SessionResult login(LoginRequest request, DeviceInfo deviceInfo) {
        User user = userRepository.findByEmail(request.email()).orElse(null);
        if (user == null) {
            loginHistoryService.record(null, request.email(), LoginAction.LOGIN_FAILED, LoginStatus.FAILED,
                    deviceInfo, null, "Không tìm thấy tài khoản");
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        boolean autoLocked = user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now());
        if (autoLocked || user.getStatus() == UserStatus.LOCKED) {
            loginHistoryService.record(user.getId(), user.getEmail(), LoginAction.LOGIN_FAILED, LoginStatus.FAILED,
                    deviceInfo, null, autoLocked ? "Tài khoản đang bị tạm khóa" : "Tài khoản đã bị admin khóa");
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            registerFailedLogin(user);
            loginHistoryService.record(user.getId(), user.getEmail(), LoginAction.LOGIN_FAILED, LoginStatus.FAILED,
                    deviceInfo, null, "Sai mật khẩu");
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        List<String> roleCodes = roleRepository.findRoleCodesByUserId(user.getId());
        List<String> permissionCodes = roleRepository.findPermissionCodesByUserId(user.getId());

        String token = jwtService.generateToken(user, roleCodes, permissionCodes);
        loginHistoryService.record(user.getId(), user.getEmail(), LoginAction.LOGIN, LoginStatus.SUCCESS,
                deviceInfo, null, null);
        AuthResponse body = AuthResponse.of(token, UserResponse.from(user, roleCodes, permissionCodes));
        ResponseCookie cookie = refreshTokenService.issueCookie(user.getId(), deviceInfo);
        return new SessionResult(body, cookie);
    }

    /** Dang nhap sai: tang bo dem, tu khoa tam MAX_FAILED_LOGIN_ATTEMPTS lan lien tiep. */
    private void registerFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plus(LOCK_DURATION));
        }
        userRepository.save(user);
    }

    /** Doi mat khau khi dang dang nhap - khac reset (khong qua email), yeu cau biet mat khau cu. Thu hoi het session, kem session hien tai. */
    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword, DeviceInfo deviceInfo) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Mật khẩu hiện tại không đúng");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);

        refreshTokenService.revokeAllByUserId(userId);
        loginHistoryService.record(userId, user.getEmail(), LoginAction.PASSWORD_CHANGED, LoginStatus.SUCCESS,
                deviceInfo, null, null);
    }

    /** Cap access token moi tu userId da duoc RefreshTokenService xac thuc qua refresh token cookie. */
    @Transactional(readOnly = true)
    public AuthResponse refreshAccessToken(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Người dùng không tồn tại"));

        List<String> roleCodes = roleRepository.findRoleCodesByUserId(user.getId());
        List<String> permissionCodes = roleRepository.findPermissionCodesByUserId(user.getId());

        String token = jwtService.generateToken(user, roleCodes, permissionCodes);
        return AuthResponse.of(token, UserResponse.from(user, roleCodes, permissionCodes));
    }

    /**
     * Goi khi access token het han. Luu y: rotate() da doi/thu hoi refresh
     * token cu tren Redis truoc khi biet chac userId con hop le - neu
     * refreshAccessToken ben duoi that bai (VD user bi xoa giua chung), token
     * cu da mat va khong the rollback (Redis khong chung transaction voi
     * Postgres). Chap nhan duoc o quy mo du an nay: truong hop nay hiem (user
     * bi xoa dung luc dang refresh) va hau qua chi la phai dang nhap lai.
     */
    public SessionResult refresh(String refreshToken, DeviceInfo deviceInfo) {
        if (refreshToken == null) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_MISSING);
        }
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(refreshToken, deviceInfo)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));

        AuthResponse body = refreshAccessToken(rotation.userId());
        loginHistoryService.record(rotation.userId(), body.user().email(), LoginAction.REFRESH_TOKEN,
                LoginStatus.SUCCESS, deviceInfo, null, null);

        return new SessionResult(body, rotation.cookie());
    }

    /** Dang xuat 1 thiet bi - luon tra ve cookie het han de trinh duyet xoa cookie cu, ke ca khi refreshToken thieu/khong hop le. */
    public ResponseCookie logout(String refreshToken, DeviceInfo deviceInfo) {
        if (refreshToken != null) {
            refreshTokenService.revoke(refreshToken)
                    .ifPresent(userId -> loginHistoryService.record(userId, null, LoginAction.LOGOUT,
                            LoginStatus.SUCCESS, deviceInfo, null, null));
        }
        return refreshTokenService.expiredCookie();
    }

    /** Dang xuat tat ca thiet bi - VD nghi ngo bi lo tai khoan. */
    public ResponseCookie logoutAll(UUID userId, DeviceInfo deviceInfo) {
        refreshTokenService.revokeAllByUserId(userId);
        loginHistoryService.record(userId, null, LoginAction.LOGOUT_ALL, LoginStatus.SUCCESS,
                deviceInfo, null, null);
        return refreshTokenService.expiredCookie();
    }

    /** Danh sach thiet bi/phien dang dang nhap cua chinh minh. */
    public List<SessionResponse> listSessions(UUID userId) {
        return refreshTokenService.listSessions(userId).stream().map(SessionResponse::from).toList();
    }

    /** Thu hoi 1 thiet bi cu the (khong nhat thiet la thiet bi dang goi request nay). */
    public void revokeSession(UUID userId, String sessionId, DeviceInfo deviceInfo) {
        boolean revoked = refreshTokenService.revokeSessionForUser(userId, sessionId);
        if (!revoked) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy phiên đăng nhập này");
        }
        loginHistoryService.record(userId, null, LoginAction.TOKEN_REVOKED, LoginStatus.SUCCESS,
                deviceInfo, sessionId, null);
    }
}
