package com.tiktokinsight.auth.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.auth")
public record AuthProperties(
        String issuer,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        String accessSecret,
        String refreshSecret,
        boolean refreshCookieSecure,
        InitialAdmin initialAdmin
) {
    public AuthProperties {
        requireSecret(accessSecret, "JWT_ACCESS_SECRET");
        requireSecret(refreshSecret, "JWT_REFRESH_SECRET");
    }

    private static void requireSecret(String value, String name) {
        if (value == null || value.length() < 32) {
            throw new IllegalArgumentException(name + " must contain at least 32 characters");
        }
    }

    public record InitialAdmin(String email, String username, String password) {
        public boolean isConfigured() {
            return email != null && !email.isBlank()
                    && username != null && !username.isBlank()
                    && password != null && !password.isBlank();
        }
    }
}
