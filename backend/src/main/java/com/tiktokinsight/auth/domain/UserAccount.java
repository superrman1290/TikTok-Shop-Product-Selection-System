package com.tiktokinsight.auth.domain;

import java.time.Instant;

public record UserAccount(
        Long id,
        String email,
        String username,
        String passwordHash,
        UserRole role,
        UserStatus status,
        Instant lockedUntil,
        Instant passwordChangedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public boolean isEnabled() {
        return status == UserStatus.ENABLED;
    }

    public boolean isLockedAt(Instant instant) {
        return lockedUntil != null && lockedUntil.isAfter(instant);
    }
}
