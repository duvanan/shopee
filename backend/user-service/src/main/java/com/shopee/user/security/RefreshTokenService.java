package com.shopee.user.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Refresh token la chuoi ngau nhien khong mang thong tin (opaque), luu o
 * Redis theo hash(token) -> userId voi TTL. Khong dung JWT lam refresh token
 * de co the thu hoi (revoke) truoc han - JWT thuong khong lam duoc dieu nay
 * neu khong luu them state.
 *
 * Rotation: moi lan resolveAndRevoke() thanh cong se xoa ngay token cu, noi
 * goi (AuthController) phai issueCookie() mot token moi. Token cu bi dung lai
 * (replay) se khong con ton tai trong Redis -> tra ve rong, bi coi la 401.
 */
@Service
public class RefreshTokenService {

    private static final String COOKIE_NAME = "refreshToken";
    private static final String REDIS_KEY_PREFIX = "refresh:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;
    private final boolean cookieSecure;

    public RefreshTokenService(StringRedisTemplate redisTemplate,
                                @Value("${jwt.refresh-expiration-days:30}") long refreshExpirationDays,
                                @Value("${app.cookie.secure:false}") boolean cookieSecure) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofDays(refreshExpirationDays);
        this.cookieSecure = cookieSecure;
    }

    /** Sinh refresh token moi cho user, luu Redis, tra ve duoi dang Set-Cookie httpOnly san sang gan vao response. */
    public ResponseCookie issueCookie(UUID userId) {
        String rawToken = generateOpaqueToken();
        redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + hash(rawToken), userId.toString(), ttl);
        return cookieBuilder(rawToken).maxAge(ttl).build();
    }

    /** Xac thuc token, neu hop le thi thu hoi ngay (rotation) va tra ve userId tuong ung. */
    public Optional<UUID> resolveAndRevoke(String rawToken) {
        String key = REDIS_KEY_PREFIX + hash(rawToken);
        String userId = redisTemplate.opsForValue().get(key);
        if (userId == null) {
            return Optional.empty();
        }
        redisTemplate.delete(key);
        return Optional.of(UUID.fromString(userId));
    }

    /** Dang xuat - thu hoi refresh token hien tai, khong sinh token moi. */
    public void revoke(String rawToken) {
        redisTemplate.delete(REDIS_KEY_PREFIX + hash(rawToken));
    }

    /** Cookie rong, het han ngay - dung khi logout de trinh duyet xoa cookie cu. */
    public ResponseCookie expiredCookie() {
        return cookieBuilder("").maxAge(0).build();
    }

    public static String cookieName() {
        return COOKIE_NAME;
    }

    private ResponseCookie.ResponseCookieBuilder cookieBuilder(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/api/users/auth");
    }

    private static String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
