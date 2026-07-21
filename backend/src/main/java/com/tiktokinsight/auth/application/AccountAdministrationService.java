package com.tiktokinsight.auth.application;

import com.tiktokinsight.auth.domain.RefreshTokenRepository;
import com.tiktokinsight.auth.domain.UserAccountRepository;
import com.tiktokinsight.auth.domain.UserStatus;
import com.tiktokinsight.common.api.ApiErrorCode;
import com.tiktokinsight.common.exception.ApiException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountAdministrationService {

    private final UserAccountRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    public AccountAdministrationService(
            UserAccountRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
    }

    @Transactional
    public AccessPrincipal updateStatus(long userId, UserStatus status) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.USER_NOT_FOUND));
        Instant now = clock.instant();
        userRepository.updateStatus(userId, status, now);
        if (status == UserStatus.DISABLED) {
            refreshTokenRepository.revokeAllForUser(userId, now);
        }
        var updated = userRepository.findById(userId).orElseThrow();
        return new AccessPrincipal(updated.id(), updated.email(), updated.username(), updated.role());
    }
}
