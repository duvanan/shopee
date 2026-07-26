package com.shopee.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopee.gateway.config.AuthorizationProperties;
import com.shopee.gateway.config.AuthorizationProperties.RouteRule;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RBAC tap trung o Gateway - xem docs/architecture/rbac.md muc 6.
 *
 * 3 nhom route (khai bao trong config-repo/api-gateway.yml, doc qua
 * AuthorizationProperties), khop theo thu tu tu chat den long de tranh
 * 1 path khop nhieu nhom gay mo ho:
 *   1. permissionRequired - can token HOP LE + du permission, thieu -> 403
 *   2. publicRoutes        - khong can token
 *   3. authenticatedOnly   - can token hop le, khong can permission cu the
 *   4. khong khop nhom nao -> mac dinh coi nhu authenticatedOnly (an toan hon
 *      mac dinh public - route moi them ma quen khai bao se bi chan thay vi lo ra ngoai)
 *
 * Service phia sau khong tu parse JWT, chi tin header X-User-Id/X-User-Roles/
 * X-User-Permissions do Gateway gan vao sau khi xac thuc.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final SecretKey signingKey;
    private final AuthorizationProperties authorizationProperties;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(@Value("${jwt.secret}") String secret,
                                    AuthorizationProperties authorizationProperties,
                                    ObjectMapper objectMapper) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.authorizationProperties = authorizationProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        RouteRule permissionRule = findMatch(authorizationProperties.getPermissionRequired(), path, method);
        if (permissionRule != null) {
            return authenticate(exchange, chain, permissionRule.getPermission());
        }

        if (findMatch(authorizationProperties.getPublicRoutes(), path, method) != null) {
            return chain.filter(exchange);
        }

        // authenticatedOnly hoac khong khop nhom nao (mac dinh an toan) deu can token,
        // chi khac o cho khong doi hoi permission cu the.
        return authenticate(exchange, chain, null);
    }

    private RouteRule findMatch(List<RouteRule> rules, String path, HttpMethod method) {
        return rules.stream()
                .filter(rule -> PATH_MATCHER.match(rule.getPath(), path))
                .filter(rule -> rule.getMethod() == null || HttpMethod.valueOf(rule.getMethod()).equals(method))
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private Mono<Void> authenticate(ServerWebExchange exchange, GatewayFilterChain chain, String requiredPermission) {
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return respondWith(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                    "Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn");
        }

        try {
            String token = authHeader.substring(7);
            var claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            List<String> roles = claims.get("roles", List.class);
            List<String> permissions = claims.get("permissions", List.class);

            if (requiredPermission != null && (permissions == null || !permissions.contains(requiredPermission))) {
                return respondWith(exchange, HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                        "Bạn không có quyền thực hiện chức năng này");
            }

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", claims.getSubject())
                    .header("X-User-Roles", roles == null ? "" : String.join(",", roles))
                    .header("X-User-Permissions", permissions == null ? "" : String.join(",", permissions))
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (Exception e) {
            return respondWith(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                    "Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn");
        }
    }

    /**
     * Gateway khong dung chung module `common` (spring-webmvc trong common
     * xung dot voi Spring Cloud Gateway - stack WebFlux/Netty), nen tu dung
     * Jackson dung format ApiResponse<Void> ngay tai day thay vi throw
     * BusinessException. Xem docs/architecture/api-response-standard.md muc 21.
     */
    private Mono<Void> respondWith(ServerWebExchange exchange, HttpStatus status, String code, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String traceId = exchange.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
        String path = exchange.getRequest().getURI().getPath();

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("timestamp", Instant.now().toString());
        meta.put("traceId", traceId);
        meta.put("path", path);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("code", code);
        body.put("message", message);
        body.put("meta", meta);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            return response.setComplete();
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
