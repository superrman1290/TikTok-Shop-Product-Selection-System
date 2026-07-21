package com.tiktokinsight.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tiktokinsight.auth.domain.LoginAttemptRepository;
import com.tiktokinsight.auth.domain.LoginThrottle;
import com.tiktokinsight.auth.domain.RefreshToken;
import com.tiktokinsight.auth.domain.RefreshTokenRepository;
import com.tiktokinsight.auth.domain.UserAccount;
import com.tiktokinsight.auth.domain.UserAccountRepository;
import com.tiktokinsight.auth.domain.UserRole;
import com.tiktokinsight.auth.domain.UserStatus;
import com.tiktokinsight.auth.infrastructure.AuthProperties;
import com.tiktokinsight.auth.infrastructure.SecureHashing;
import com.tiktokinsight.auth.infrastructure.TokenService;
import com.tiktokinsight.common.api.ApiErrorCode;
import com.tiktokinsight.common.exception.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-21T08:30:00Z");
    private static final String PASSWORD = "Password1";
    private final UserAccountRepository userRepository = mock(UserAccountRepository.class);
    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final LoginAttemptRepository loginAttemptRepository = mock(LoginAttemptRepository.class);
    private final LoginThrottle loginThrottle = mock(LoginThrottle.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final AuthProperties properties = new AuthProperties(
            "test-issuer",
            Duration.ofHours(2),
            Duration.ofDays(30),
            "test_access_secret_with_at_least_32_characters",
            "test_refresh_secret_with_at_least_32_characters",
            false,
            new AuthProperties.InitialAdmin("", "", "")
    );
    private final SecureHashing hashing = new SecureHashing(properties);
    private final TokenService tokenService = new TokenService(properties);
    private final AtomicLong tokenIds = new AtomicLong(10);
    private AuthService service;

    @BeforeEach
    void setUp() {
        when(loginThrottle.isIpBlocked(anyString())).thenReturn(false);
        when(refreshTokenRepository.create(anyLong(), anyString(), any(), any(), anyString()))
                .thenAnswer(invocation -> new RefreshToken(
                        tokenIds.incrementAndGet(),
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        invocation.getArgument(2),
                        null,
                        null,
                        invocation.getArgument(3),
                        invocation.getArgument(4)
                ));
        service = new AuthService(
                userRepository,
                refreshTokenRepository,
                loginAttemptRepository,
                loginThrottle,
                passwordEncoder,
                tokenService,
                hashing,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void registersNormalizedUserAndPersistsOnlyRefreshHash() {
        UserAccount created = user(1, "buyer@example.com", UserRole.USER, UserStatus.ENABLED, null);
        when(userRepository.create(eq("buyer@example.com"), eq("Buyer"), anyString(),
                eq(UserRole.USER), eq(UserStatus.ENABLED), eq(NOW))).thenReturn(created);

        AuthSession session = service.register(" Buyer@Example.com ", " Buyer ", PASSWORD, "203.0.113.5");

        assertThat(session.user().email()).isEqualTo("buyer@example.com");
        assertThat(session.tokens().refreshToken()).isNotBlank();
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(refreshTokenRepository).create(eq(1L), hashCaptor.capture(), eq(NOW.plus(Duration.ofDays(30))),
                eq(NOW), anyString());
        assertThat(hashCaptor.getValue()).hasSize(64).doesNotContain(session.tokens().refreshToken());
    }

    @Test
    void rejectsDuplicateEmailAndWeakPassword() {
        when(userRepository.create(anyString(), anyString(), anyString(), any(), any(), any()))
                .thenThrow(new DuplicateKeyException("duplicate"));

        assertApiError(() -> service.register("buyer@example.com", "Buyer", PASSWORD, "ip"),
                ApiErrorCode.EMAIL_EXISTS);
        assertApiError(() -> service.register("buyer@example.com", "Buyer", "lettersOnly", "ip"),
                ApiErrorCode.VALIDATION_FAILED);
    }

    @Test
    void logsInEnabledUserAndClearsAccountFailures() {
        UserAccount user = user(1, "buyer@example.com", UserRole.USER, UserStatus.ENABLED, null);
        when(userRepository.findByEmail(user.email())).thenReturn(Optional.of(user));

        AuthSession session = service.login(user.email(), PASSWORD, "203.0.113.5");

        assertThat(session.user().userId()).isEqualTo(1);
        verify(loginAttemptRepository).record(eq(1L), anyString(), anyString(), eq(true), eq(NOW));
        verify(loginThrottle).clearAccountFailures(anyString());
    }

    @Test
    void locksAccountAtFifthFailureAndRejectsLockedAccount() {
        UserAccount user = user(1, "buyer@example.com", UserRole.USER, UserStatus.ENABLED, null);
        when(userRepository.findByEmail(user.email())).thenReturn(Optional.of(user));
        when(loginThrottle.recordFailure(anyString(), anyString()))
                .thenReturn(new LoginThrottle.FailureResult(false, false))
                .thenReturn(new LoginThrottle.FailureResult(false, false))
                .thenReturn(new LoginThrottle.FailureResult(false, false))
                .thenReturn(new LoginThrottle.FailureResult(false, false))
                .thenReturn(new LoginThrottle.FailureResult(true, false));

        for (int attempt = 0; attempt < 5; attempt++) {
            assertApiError(() -> service.login(user.email(), "WrongPassword1", "203.0.113.5"),
                    ApiErrorCode.INVALID_CREDENTIALS);
        }
        verify(userRepository).updateLockedUntil(1, NOW.plus(Duration.ofMinutes(15)), NOW);

        UserAccount locked = user(1, user.email(), UserRole.USER, UserStatus.ENABLED,
                NOW.plus(Duration.ofMinutes(15)));
        when(userRepository.findByEmail(user.email())).thenReturn(Optional.of(locked));
        assertApiError(() -> service.login(user.email(), PASSWORD, "203.0.113.5"),
                ApiErrorCode.ACCOUNT_LOCKED);
    }

    @Test
    void blocksIpAndDisabledAccount() {
        when(loginThrottle.isIpBlocked(anyString())).thenReturn(true);
        assertApiError(() -> service.login("buyer@example.com", PASSWORD, "203.0.113.5"),
                ApiErrorCode.LOGIN_RATE_LIMITED);
        verify(userRepository, never()).findByEmail(anyString());

        when(loginThrottle.isIpBlocked(anyString())).thenReturn(false);
        UserAccount disabled = user(2, "disabled@example.com", UserRole.USER, UserStatus.DISABLED, null);
        when(userRepository.findByEmail(disabled.email())).thenReturn(Optional.of(disabled));
        assertApiError(() -> service.login(disabled.email(), PASSWORD, "203.0.113.6"),
                ApiErrorCode.ACCOUNT_DISABLED);
    }

    @Test
    void rotatesRefreshTokenAndRejectsTheOldTokenAfterRevocation() {
        UserAccount user = user(1, "buyer@example.com", UserRole.USER, UserStatus.ENABLED, null);
        String oldRawToken = "old-refresh-token";
        String oldHash = hashing.refreshTokenHash(oldRawToken);
        RefreshToken oldToken = new RefreshToken(
                7L, user.id(), oldHash, NOW.plus(Duration.ofDays(1)), null, null, NOW.minusSeconds(30), "ip"
        );
        when(refreshTokenRepository.findByHashForUpdate(oldHash))
                .thenReturn(Optional.of(oldToken))
                .thenReturn(Optional.empty());
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));

        AuthSession replacement = service.refresh(oldRawToken, "203.0.113.5");

        assertThat(replacement.tokens().refreshToken()).isNotEqualTo(oldRawToken);
        verify(refreshTokenRepository).revoke(eq(7L), eq(NOW), anyLong());
        assertApiError(() -> service.refresh(oldRawToken, "203.0.113.5"),
                ApiErrorCode.REFRESH_TOKEN_INVALID);
    }

    @Test
    void revokesRefreshTokensOnLogoutPasswordChangeAndDisabledRefresh() {
        UserAccount user = user(1, "buyer@example.com", UserRole.USER, UserStatus.ENABLED, null);
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));
        service.changePassword(user.id(), PASSWORD, "NewPassword2");
        verify(userRepository).updatePassword(eq(user.id()), anyString(), eq(NOW));
        verify(refreshTokenRepository).revokeAllForUser(user.id(), NOW);

        String raw = "active-token";
        RefreshToken token = new RefreshToken(8L, user.id(), hashing.refreshTokenHash(raw),
                NOW.plusSeconds(60), null, null, NOW, "ip");
        when(refreshTokenRepository.findByHashForUpdate(token.tokenHash())).thenReturn(Optional.of(token));
        service.logout(raw);
        service.logout(null);
        verify(refreshTokenRepository).revoke(8L, NOW, null);

        UserAccount disabled = user(1, user.email(), UserRole.USER, UserStatus.DISABLED, null);
        when(userRepository.findById(user.id())).thenReturn(Optional.of(disabled));
        assertApiError(() -> service.refresh(raw, "ip"), ApiErrorCode.ACCOUNT_DISABLED);
    }

    @Test
    void returnsCurrentUserAndRejectsMissingUserOrBadCurrentPassword() {
        UserAccount user = user(1, "buyer@example.com", UserRole.USER, UserStatus.ENABLED, null);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        assertThat(service.currentUser(1).username()).isEqualTo("Buyer");
        assertApiError(() -> service.changePassword(1, "WrongPassword1", "NewPassword2"),
                ApiErrorCode.INVALID_CREDENTIALS);

        when(userRepository.findById(99)).thenReturn(Optional.empty());
        assertApiError(() -> service.currentUser(99), ApiErrorCode.USER_NOT_FOUND);
    }

    private UserAccount user(long id, String email, UserRole role, UserStatus status, Instant lockedUntil) {
        return new UserAccount(
                id,
                email,
                "Buyer",
                passwordEncoder.encode(PASSWORD),
                role,
                status,
                lockedUntil,
                NOW.minusSeconds(60),
                NOW.minusSeconds(60),
                NOW.minusSeconds(60)
        );
    }

    private void assertApiError(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable, ApiErrorCode code) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(code));
    }
}
