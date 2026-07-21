package com.tiktokinsight.auth.domain;

import java.time.Instant;

public record RefreshToken(
        Long id,
        long userId,
        String tokenHash,
        Instant expiresAt,
        Instant revokedAt,
        Long replacedByTokenId,
        Instant createdAt,
        String createdByIpHash
) {
    public boolean isActiveAt(Instant instant) {
        return revokedAt == null && expiresAt.isAfter(instant);
    }
}
