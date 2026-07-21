package com.tiktokinsight.auth.domain;

import java.time.Instant;

public interface LoginAttemptRepository {
    void record(
            Long userId,
            String accountKeyHash,
            String ipAddressHash,
            boolean successful,
            Instant attemptedAt
    );
}
