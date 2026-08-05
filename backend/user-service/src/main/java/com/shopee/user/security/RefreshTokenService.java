package com.shopee.user.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Refresh token la chuoi ngau nhien khong mang thong tin (opaque). Moi lan
 * dang nhap tao 1 "session" tren Redis (session con song qua nhieu lan
 * refresh - chi token value doi, sessionId giu nguyen), cho phep nguoi dung
 * xem/thu hoi tung thiet bi dang dang nhap (GET/DELETE
 * /api/users/auth/sessions) thay vi chi co 1 token -> userId nhu truoc.
 *
 * Redis key:
 *   refresh:session:{sessionId}      -> JSON cua RefreshSession, TTL = ttl
 *   refresh:lookup:{tokenHash}       -> sessionId,                TTL = ttl
 *   refresh:user-sessions:{userId}   -> SET cac sessionId cua user (khong TTL,
 *                                        don rac lazy khi listSessions() thay
 *                                        session con tro toi da het han)
 *
 * Khong lam: phat hien "reuse" cua token da rotate (token cu bi dung lai sau
 * khi da doi token moi) de tu dong thu hoi ca session - can them 1 lop
 * "tombstone" key rieng, chua can thiet o quy mo du an nay. Hien tai token cu
 * chi don gian khong con hop le sau khi rotate (tra REFRESH_TOKEN_INVALID),
 * giong het hanh vi rotation truoc day.
 */
@Service
public class RefreshTokenService {

    private static final String COOKIE_NAME = "refreshToken";
    private static final String SESSION_KEY_PREFIX = "refresh:session:";
    private static final String LOOKUP_KEY_PREFIX = "refresh:lookup:";
    private static final String USER_SESSIONS_KEY_PREFIX = "refresh:user-sessions:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;
    private final boolean cookieSecure;

    public RefreshTokenService(StringRedisTemplate redisTemplate,
                                ObjectMapper objectMapper,
                                @Value("${jwt.refresh-expiration-days:30}") long refreshExpirationDays,
                                @Value("${app.cookie.secure:false}") boolean cookieSecure) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofDays(refreshExpirationDays);
        this.cookieSecure = cookieSecure;
    }

    /** Dang nhap/dang ky - luon tao session moi. */
    public ResponseCookie issueCookie(UUID userId, DeviceInfo deviceInfo) {
        String rawToken = TokenHasher.generateOpaqueToken();
        Instant now = Instant.now();
        RefreshSession session = new RefreshSession(
                UUID.randomUUID().toString(), userId, TokenHasher.hash(rawToken),
                deviceInfo.ipAddress(), deviceInfo.userAgent(), now, now.plus(ttl), now);

        saveSession(session);
        return cookieBuilder(rawToken).maxAge(ttl).build();
    }

    /** Ket qua rotate() - userId de cap access token moi, cookie de tra ve client. */
    public record RotationResult(UUID userId, ResponseCookie cookie) {
    }

    /** Goi khi access token het han - giu nguyen sessionId, chi doi token value (rotation). */
    public Optional<RotationResult> rotate(String rawToken, DeviceInfo deviceInfo) {
        String oldHash = TokenHasher.hash(rawToken);
        String sessionId = redisTemplate.opsForValue().get(LOOKUP_KEY_PREFIX + oldHash);
        if (sessionId == null) {
            return Optional.empty();
        }
        RefreshSession session = readSession(sessionId);
        if (session == null) {
            return Optional.empty();
        }

        String newRawToken = TokenHasher.generateOpaqueToken();
        Instant now = Instant.now();

        redisTemplate.delete(LOOKUP_KEY_PREFIX + oldHash);
        saveSession(new RefreshSession(sessionId, session.userId(), TokenHasher.hash(newRawToken),
                deviceInfo.ipAddress(), deviceInfo.userAgent(), session.issuedAt(), now.plus(ttl), now));

        return Optional.of(new RotationResult(session.userId(), cookieBuilder(newRawToken).maxAge(ttl).build()));
    }

    /** Dang xuat 1 thiet bi - thu hoi theo raw token trong cookie hien tai. Tra ve userId neu tim thay de ghi login history. */
    public Optional<UUID> revoke(String rawToken) {
        String hash = TokenHasher.hash(rawToken);
        String sessionId = redisTemplate.opsForValue().get(LOOKUP_KEY_PREFIX + hash);
        redisTemplate.delete(LOOKUP_KEY_PREFIX + hash);
        if (sessionId == null) {
            return Optional.empty();
        }
        RefreshSession session = readSession(sessionId);
        redisTemplate.delete(SESSION_KEY_PREFIX + sessionId);
        if (session == null) {
            return Optional.empty();
        }
        redisTemplate.opsForSet().remove(USER_SESSIONS_KEY_PREFIX + session.userId(), sessionId);
        return Optional.of(session.userId());
    }

    /** Dang xuat tat ca thiet bi - VD khi doi mat khau/khoa tai khoan (xem thiet ke muc 12.14, 12.15). */
    public void revokeAllByUserId(UUID userId) {
        String userSessionsKey = USER_SESSIONS_KEY_PREFIX + userId;
        Set<String> sessionIds = redisTemplate.opsForSet().members(userSessionsKey);
        if (sessionIds != null) {
            for (String sessionId : sessionIds) {
                RefreshSession session = readSession(sessionId);
                if (session != null) {
                    redisTemplate.delete(LOOKUP_KEY_PREFIX + session.refreshTokenHash());
                }
                redisTemplate.delete(SESSION_KEY_PREFIX + sessionId);
            }
        }
        redisTemplate.delete(userSessionsKey);
    }

    /** Danh sach session dang hoat dong cua user - GET /api/users/auth/sessions. */
    public List<RefreshSession> listSessions(UUID userId) {
        String userSessionsKey = USER_SESSIONS_KEY_PREFIX + userId;
        Set<String> sessionIds = redisTemplate.opsForSet().members(userSessionsKey);
        List<RefreshSession> sessions = new ArrayList<>();
        if (sessionIds == null) {
            return sessions;
        }
        for (String sessionId : sessionIds) {
            RefreshSession session = readSession(sessionId);
            if (session == null) {
                // Session da het han/bi xoa nhung sessionId con sot trong set - don luon.
                redisTemplate.opsForSet().remove(userSessionsKey, sessionId);
            } else {
                sessions.add(session);
            }
        }
        return sessions;
    }

    /** Thu hoi 1 session cu the (DELETE /api/users/auth/sessions/{id}) - chi thanh cong neu session thuoc dung user. */
    public boolean revokeSessionForUser(UUID userId, String sessionId) {
        RefreshSession session = readSession(sessionId);
        if (session == null || !session.userId().equals(userId)) {
            return false;
        }
        redisTemplate.delete(LOOKUP_KEY_PREFIX + session.refreshTokenHash());
        redisTemplate.delete(SESSION_KEY_PREFIX + sessionId);
        redisTemplate.opsForSet().remove(USER_SESSIONS_KEY_PREFIX + userId, sessionId);
        return true;
    }

    /** Cookie rong, het han ngay - dung khi logout de trinh duyet xoa cookie cu. */
    public ResponseCookie expiredCookie() {
        return cookieBuilder("").maxAge(0).build();
    }

    public static String cookieName() {
        return COOKIE_NAME;
    }

    private void saveSession(RefreshSession session) {
        redisTemplate.opsForValue().set(SESSION_KEY_PREFIX + session.sessionId(), writeJson(session), ttl);
        redisTemplate.opsForValue().set(LOOKUP_KEY_PREFIX + session.refreshTokenHash(), session.sessionId(), ttl);
        redisTemplate.opsForSet().add(USER_SESSIONS_KEY_PREFIX + session.userId(), session.sessionId());
    }

    private RefreshSession readSession(String sessionId) {
        String json = redisTemplate.opsForValue().get(SESSION_KEY_PREFIX + sessionId);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, RefreshSession.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String writeJson(RefreshSession session) {
        try {
            return objectMapper.writeValueAsString(session);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Khong serialize duoc RefreshSession", e);
        }
    }

    private ResponseCookie.ResponseCookieBuilder cookieBuilder(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/api/users/auth");
    }

}
