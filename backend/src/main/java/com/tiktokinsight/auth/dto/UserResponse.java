package com.tiktokinsight.auth.dto;

import com.tiktokinsight.auth.application.AccessPrincipal;

public record UserResponse(long id, String email, String username, String role) {
    public static UserResponse from(AccessPrincipal principal) {
        return new UserResponse(
                principal.userId(),
                principal.email(),
                principal.username(),
                principal.role().name()
        );
    }
}
