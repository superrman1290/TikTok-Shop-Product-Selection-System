package com.tiktokinsight.auth.domain;

import java.time.Instant;
import java.util.Optional;

public interface UserAccountRepository {
    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findById(long userId);

    UserAccount create(
            String email,
            String username,
            String passwordHash,
            UserRole role,
            UserStatus status,
            Instant now
    );

    void updateLockedUntil(long userId, Instant lockedUntil, Instant now);

    void updatePassword(long userId, String passwordHash, Instant now);

    void updateStatus(long userId, UserStatus status, Instant now);
}
