package com.shopee.user.dto;

import com.shopee.user.entity.AuditLog;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID actorId,
        String actorEmail,
        String actorType,
        String action,
        String module,
        String objectType,
        String objectId,
        String description,
        String oldValue,
        String newValue,
        String status,
        Instant createdAt
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(), log.getActorId(), log.getActorEmail(),
                log.getActorType().name(), log.getAction().name(), log.getModule(),
                log.getObjectType(), log.getObjectId(), log.getDescription(),
                log.getOldValue(), log.getNewValue(), log.getStatus().name(), log.getCreatedAt());
    }
}
