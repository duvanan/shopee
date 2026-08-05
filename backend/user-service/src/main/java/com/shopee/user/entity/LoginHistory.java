package com.shopee.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Ghi lai lich su dang nhap/dang xuat lau dai (xem
 * docs/architecture/user-auth-role-permission-redis-refresh-token-design-uuid.md
 * muc 14) - khac voi session tren Redis vi session co the het han/bi xoa.
 * Bang chi insert, khong sua nen khong ke thua Auditable (khong can updated_at).
 */
@Entity
@Table(name = "login_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LoginAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LoginStatus status;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "trace_id")
    private String traceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
