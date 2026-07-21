package com.tiktokinsight.auth.dto;

import com.tiktokinsight.auth.application.AuthSession;
import java.time.Instant;

public record AuthResponse(
        String accessToken,
        String tokenType,
        Instant accessExpiresAt,
        UserResponse user
) {
    public static AuthResponse from(AuthSession session) {
        return new AuthResponse(
                session.tokens().accessToken(),
                "Bearer",
                session.tokens().accessExpiresAt(),
                UserResponse.from(session.user())
        );
    }
}
