package com.tiktokinsight.auth.application;

public record AuthSession(IssuedTokens tokens, AccessPrincipal user) {
}
