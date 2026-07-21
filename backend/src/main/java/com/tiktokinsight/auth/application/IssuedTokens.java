package com.tiktokinsight.auth.application;

import java.time.Instant;

public record IssuedTokens(String accessToken, String refreshToken, Instant accessExpiresAt) {
}
