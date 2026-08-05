package com.shopee.user.security;

import java.time.Instant;
import java.util.UUID;

/**
 * Gia tri JSON luu tren Redis theo key refresh:session:{sessionId} - xem
 * docs/architecture/user-auth-role-permission-redis-refresh-token-design-uuid.md
 * muc 12.7. Khong luu tokenFamily/status: revoke = xoa key thang tren Redis
 * (con ton tai == active), khong soft-mark; phat hien reuse-attack cua token
 * da rotate chua lam o phase nay (xem ghi chu trong RefreshTokenService).
 */
public record RefreshSession(
        String sessionId,
        UUID userId,
        String refreshTokenHash,
        String ipAddress,
        String userAgent,
        Instant issuedAt,
        Instant expiresAt,
        Instant lastUsedAt
) {
}
