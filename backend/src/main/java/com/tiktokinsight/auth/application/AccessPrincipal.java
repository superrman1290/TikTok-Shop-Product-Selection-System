package com.tiktokinsight.auth.application;

import com.tiktokinsight.auth.domain.UserRole;

public record AccessPrincipal(long userId, String email, String username, UserRole role) {
}
