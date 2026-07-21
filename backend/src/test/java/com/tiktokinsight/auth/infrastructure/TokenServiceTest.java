package com.tiktokinsight.auth.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tiktokinsight.auth.application.AccessPrincipal;
import com.tiktokinsight.auth.application.IssuedTokens;
import com.tiktokinsight.auth.domain.UserAccount;
import com.tiktokinsight.auth.domain.UserRole;
import com.tiktokinsight.auth.domain.UserStatus;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TokenServiceTest {

    private final AuthProperties properties = new AuthProperties(
            "test-issuer", Duration.ofHours(2), Duration.ofDays(30),
            "test_access_secret_with_at_least_32_characters",
            "test_refresh_secret_with_at_least_32_characters",
            false, new AuthProperties.InitialAdmin("", "", "")
    );

    @Test
    void issuesAndDecodesAccessTokenWithExactLifetime() {
        TokenService service = new TokenService(properties);
        Instant now = Instant.now();
        UserAccount user = new UserAccount(42L, "admin@example.com", "Admin", "hash",
                UserRole.ADMIN, UserStatus.ENABLED, null, now, now, now);

        IssuedTokens tokens = service.issue(user, now);
        IssuedTokens secondIssue = service.issue(user, now);
        AccessPrincipal principal = service.decodeAccessToken(tokens.accessToken());

        assertThat(principal.userId()).isEqualTo(42);
        assertThat(principal.role()).isEqualTo(UserRole.ADMIN);
        assertThat(tokens.accessExpiresAt()).isEqualTo(now.plus(Duration.ofHours(2)));
        assertThat(tokens.refreshToken()).hasSize(64);
        assertThat(secondIssue.accessToken()).isNotEqualTo(tokens.accessToken());
        assertThat(secondIssue.refreshToken()).isNotEqualTo(tokens.refreshToken());
        assertThat(service.refreshExpiresAt(now)).isEqualTo(now.plus(Duration.ofDays(30)));
    }

    @Test
    void requiresSecretsOfAtLeast32Characters() {
        assertThatThrownBy(() -> new AuthProperties(
                "issuer", Duration.ofHours(2), Duration.ofDays(30), "short", "also-short",
                false, new AuthProperties.InitialAdmin("", "", "")
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void hashesRefreshTokensWithKeyedHashAndIdentifiersWithSha256() {
        SecureHashing hashing = new SecureHashing(properties);
        assertThat(hashing.refreshTokenHash("secret-token"))
                .hasSize(64)
                .isEqualTo(hashing.refreshTokenHash("secret-token"))
                .isNotEqualTo(hashing.refreshTokenHash("other-token"));
        assertThat(hashing.identifierHash("buyer@example.com")).hasSize(64);
    }
}
