package com.tiktokinsight.auth.application;

import com.tiktokinsight.auth.domain.LoginAttemptRepository;
import com.tiktokinsight.auth.domain.LoginThrottle;
import com.tiktokinsight.auth.domain.PasswordPolicy;
import com.tiktokinsight.auth.domain.RefreshToken;
import com.tiktokinsight.auth.domain.RefreshTokenRepository;
import com.tiktokinsight.auth.domain.UserAccount;
import com.tiktokinsight.auth.domain.UserAccountRepository;
import com.tiktokinsight.auth.domain.UserRole;
import com.tiktokinsight.auth.domain.UserStatus;
import com.tiktokinsight.auth.infrastructure.SecureHashing;
import com.tiktokinsight.auth.infrastructure.TokenService;
import com.tiktokinsight.common.api.ApiErrorCode;
import com.tiktokinsight.common.exception.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Duration ACCOUNT_LOCK_DURATION = Duration.ofMinutes(15);
    private final UserAccountRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final LoginThrottle loginThrottle;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final SecureHashing hashing;
    private final Clock clock;

    public AuthService(
            UserAccountRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            LoginAttemptRepository loginAttemptRepository,
            LoginThrottle loginThrottle,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            SecureHashing hashing,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.loginThrottle = loginThrottle;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.hashing = hashing;
        this.clock = clock;
    }

    @Transactional
    public AuthSession register(String email, String username, String password, String clientIp) {
        requireValidPassword(password);
        Instant now = clock.instant();
        UserAccount user;
        try {
            user = userRepository.create(
                    normalizeEmail(email),
                    username.trim(),
                    passwordEncoder.encode(password),
                    UserRole.USER,
                    UserStatus.ENABLED,
                    now
            );
        } catch (DuplicateKeyException exception) {
            throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.EMAIL_EXISTS);
        }
        return createSession(user, clientIp, now).session();
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthSession login(String email, String password, String clientIp) {
        Instant now = clock.instant();
        String normalizedEmail = normalizeEmail(email);
        String accountHash = hashing.identifierHash(normalizedEmail);
        String ipHash = hashing.identifierHash(clientIp);
        if (loginThrottle.isIpBlocked(ipHash)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, ApiErrorCode.LOGIN_RATE_LIMITED);
        }

        UserAccount user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user != null && user.isLockedAt(now)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ApiErrorCode.ACCOUNT_LOCKED);
        }
        if (user == null || !passwordEncoder.matches(password, user.passwordHash())) {
            recordFailedLogin(user, accountHash, ipHash, now);
            throw new ApiException(HttpStatus.UNAUTHORIZED, ApiErrorCode.INVALID_CREDENTIALS);
        }
        if (!user.isEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.ACCOUNT_DISABLED);
        }

        loginAttemptRepository.record(user.id(), accountHash, ipHash, true, now);
        loginThrottle.clearAccountFailures(accountHash);
        return createSession(user, clientIp, now).session();
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthSession refresh(String rawRefreshToken, String clientIp) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw invalidRefreshToken();
        }
        Instant now = clock.instant();
        RefreshToken currentToken = refreshTokenRepository
                .findByHashForUpdate(hashing.refreshTokenHash(rawRefreshToken))
                .filter(token -> token.isActiveAt(now))
                .orElseThrow(this::invalidRefreshToken);
        UserAccount user = userRepository.findById(currentToken.userId())
                .orElseThrow(this::invalidRefreshToken);
        if (!user.isEnabled()) {
            refreshTokenRepository.revokeAllForUser(user.id(), now);
            throw new ApiException(HttpStatus.FORBIDDEN, ApiErrorCode.ACCOUNT_DISABLED);
        }

        PersistedSession replacement = createSession(user, clientIp, now);
        refreshTokenRepository.revoke(currentToken.id(), now, replacement.refreshToken().id());
        return replacement.session();
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        Instant now = clock.instant();
        refreshTokenRepository.findByHashForUpdate(hashing.refreshTokenHash(rawRefreshToken))
                .filter(token -> token.isActiveAt(now))
                .ifPresent(token -> refreshTokenRepository.revoke(token.id(), now, null));
    }

    @Transactional(readOnly = true)
    public AccessPrincipal currentUser(long userId) {
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.USER_NOT_FOUND));
        return toPrincipal(user);
    }

    @Transactional
    public void changePassword(long userId, String currentPassword, String newPassword) {
        requireValidPassword(newPassword);
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.USER_NOT_FOUND));
        if (!passwordEncoder.matches(currentPassword, user.passwordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ApiErrorCode.INVALID_CREDENTIALS);
        }
        Instant now = clock.instant();
        userRepository.updatePassword(userId, passwordEncoder.encode(newPassword), now);
        refreshTokenRepository.revokeAllForUser(userId, now);
    }

    private void recordFailedLogin(UserAccount user, String accountHash, String ipHash, Instant now) {
        loginAttemptRepository.record(user == null ? null : user.id(), accountHash, ipHash, false, now);
        LoginThrottle.FailureResult result = loginThrottle.recordFailure(accountHash, ipHash);
        if (result.accountThresholdReached() && user != null) {
            userRepository.updateLockedUntil(user.id(), now.plus(ACCOUNT_LOCK_DURATION), now);
        }
        if (result.ipThresholdReached()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, ApiErrorCode.LOGIN_RATE_LIMITED);
        }
    }

    private PersistedSession createSession(UserAccount user, String clientIp, Instant now) {
        IssuedTokens tokens = tokenService.issue(user, now);
        RefreshToken refreshToken = refreshTokenRepository.create(
                user.id(),
                hashing.refreshTokenHash(tokens.refreshToken()),
                tokenService.refreshExpiresAt(now),
                now,
                hashing.identifierHash(clientIp)
        );
        return new PersistedSession(new AuthSession(tokens, toPrincipal(user)), refreshToken);
    }

    private AccessPrincipal toPrincipal(UserAccount user) {
        return new AccessPrincipal(user.id(), user.email(), user.username(), user.role());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void requireValidPassword(String password) {
        if (!PasswordPolicy.isValid(password)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED);
        }
    }

    private ApiException invalidRefreshToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, ApiErrorCode.REFRESH_TOKEN_INVALID);
    }

    private record PersistedSession(AuthSession session, RefreshToken refreshToken) {
    }
}
