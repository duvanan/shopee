package com.shopee.user.security;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Lay userId dang thao tac tu SecurityContext (do HeaderAuthenticationFilter
 * gan tu header X-User-Id) de Spring Data JPA Auditing ghi created_by/updated_by.
 * Khong co authentication (VD: tu dang ky) -> Optional.empty(), cot de NULL.
 */
@Component
public class SecurityAuditorAware implements AuditorAware<UUID> {

    @Override
    public Optional<UUID> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(authentication.getPrincipal().toString()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
