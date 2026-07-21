package com.tiktokinsight.auth.domain;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository {
    RefreshToken create(long userId, String tokenHash, Instant expiresAt, Instant now, String ipHash);

    Optional<RefreshToken> findByHashForUpdate(String tokenHash);

    void revoke(long tokenId, Instant revokedAt, Long replacementTokenId);

    void revokeAllForUser(long userId, Instant revokedAt);
}
