package com.shopee.user.service;

import com.shopee.common.exception.BusinessException;
import com.shopee.common.exception.ErrorCode;
import com.shopee.user.entity.LoginAction;
import com.shopee.user.entity.LoginStatus;
import com.shopee.user.entity.PasswordResetToken;
import com.shopee.user.entity.ResetTokenStatus;
import com.shopee.user.entity.User;
import com.shopee.user.repository.PasswordResetTokenRepository;
import com.shopee.user.repository.UserRepository;
import com.shopee.user.security.DeviceInfo;
import com.shopee.user.security.RefreshTokenService;
import com.shopee.user.security.TokenHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Luong quen/dat lai mat khau (xem
 * docs/architecture/user-auth-role-permission-redis-refresh-token-design-uuid.md
 * muc 11). CHUA co SMTP/mail provider that trong du an - buoc "gui email"
 * hien tai chi log link ra log de test duoc luc dev, thay bang mail sender
 * that khi co quyet dinh dung provider nao (SendGrid/SES/SMTP...).
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final LoginHistoryService loginHistoryService;

    public PasswordResetService(UserRepository userRepository, PasswordResetTokenRepository tokenRepository,
                                 PasswordEncoder passwordEncoder, RefreshTokenService refreshTokenService,
                                 LoginHistoryService loginHistoryService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.loginHistoryService = loginHistoryService;
    }

    /**
     * Khong bao gio bao cho caller biet email co ton tai hay khong (tranh do
     * tai khoan) - luon "thanh cong" tu goc nhin cua controller/API response,
     * kha nang khong lam gi ca neu khong tim thay user.
     */
    @Transactional
    public void requestReset(String email, DeviceInfo deviceInfo) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String rawToken = TokenHasher.generateOpaqueToken();
            PasswordResetToken token = PasswordResetToken.builder()
                    .userId(user.getId())
                    .tokenHash(TokenHasher.hash(rawToken))
                    .expiresAt(Instant.now().plus(TOKEN_TTL))
                    .status(ResetTokenStatus.ACTIVE)
                    .requestedIp(deviceInfo.ipAddress())
                    .userAgent(deviceInfo.userAgent())
                    .createdAt(Instant.now())
                    .build();
            tokenRepository.save(token);

            log.info("[password-reset] TODO gui email that - link dat lai mat khau cho {}: token={} (het han sau {} phut)",
                    user.getEmail(), rawToken, TOKEN_TTL.toMinutes());
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword, DeviceInfo deviceInfo) {
        PasswordResetToken token = tokenRepository.findByTokenHash(TokenHasher.hash(rawToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESET_TOKEN_INVALID));

        if (token.getStatus() != ResetTokenStatus.ACTIVE || token.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.RESET_TOKEN_INVALID);
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESET_TOKEN_INVALID));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);

        token.setStatus(ResetTokenStatus.USED);
        token.setUsedAt(Instant.now());
        tokenRepository.save(token);

        refreshTokenService.revokeAllByUserId(user.getId());
        loginHistoryService.record(user.getId(), user.getEmail(), LoginAction.PASSWORD_RESET, LoginStatus.SUCCESS,
                deviceInfo, null, null);
    }
}
