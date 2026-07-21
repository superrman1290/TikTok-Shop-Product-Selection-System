package com.tiktokinsight.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tiktokinsight.auth.domain.RefreshTokenRepository;
import com.tiktokinsight.auth.domain.UserAccount;
import com.tiktokinsight.auth.domain.UserAccountRepository;
import com.tiktokinsight.auth.domain.UserRole;
import com.tiktokinsight.auth.domain.UserStatus;
import com.tiktokinsight.common.api.ApiErrorCode;
import com.tiktokinsight.common.exception.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AccountAdministrationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-21T08:30:00Z");
    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final RefreshTokenRepository tokens = mock(RefreshTokenRepository.class);
    private final AccountAdministrationService service = new AccountAdministrationService(
            users, tokens, Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void disablesUserAndRevokesSessions() {
        UserAccount enabled = user(UserStatus.ENABLED);
        UserAccount disabled = user(UserStatus.DISABLED);
        when(users.findById(1)).thenReturn(Optional.of(enabled), Optional.of(disabled));

        AccessPrincipal result = service.updateStatus(1, UserStatus.DISABLED);

        assertThat(result.userId()).isEqualTo(1);
        verify(users).updateStatus(1, UserStatus.DISABLED, NOW);
        verify(tokens).revokeAllForUser(1, NOW);
    }

    @Test
    void rejectsUnknownUser() {
        when(users.findById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateStatus(99, UserStatus.DISABLED))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ApiErrorCode.USER_NOT_FOUND));
    }

    private UserAccount user(UserStatus status) {
        return new UserAccount(1L, "buyer@example.com", "Buyer", "hash", UserRole.USER, status,
                null, NOW, NOW, NOW);
    }
}
