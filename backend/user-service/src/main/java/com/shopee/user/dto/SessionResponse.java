package com.shopee.user.dto;

import com.shopee.user.security.RefreshSession;

import java.time.Instant;

/** Khong bao gio tra refreshTokenHash ra ngoai - chi field hien thi. */
public record SessionResponse(
        String sessionId,
        String ipAddress,
        String userAgent,
        Instant issuedAt,
        Instant lastUsedAt,
        Instant expiresAt
) {
    public static SessionResponse from(RefreshSession session) {
        return new SessionResponse(
                session.sessionId(),
                session.ipAddress(),
                session.userAgent(),
                session.issuedAt(),
                session.lastUsedAt(),
                session.expiresAt()
        );
    }
}
