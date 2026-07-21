package com.tiktokinsight.auth.api;

import com.tiktokinsight.auth.application.AccessPrincipal;
import com.tiktokinsight.auth.application.AuthService;
import com.tiktokinsight.auth.application.AuthSession;
import com.tiktokinsight.auth.dto.AuthResponse;
import com.tiktokinsight.auth.dto.ChangePasswordRequest;
import com.tiktokinsight.auth.dto.LoginRequest;
import com.tiktokinsight.auth.dto.RegisterRequest;
import com.tiktokinsight.auth.dto.UserResponse;
import com.tiktokinsight.auth.infrastructure.AuthProperties;
import com.tiktokinsight.common.api.ApiResponse;
import com.tiktokinsight.common.logging.RequestIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class AuthController {

    public static final String REFRESH_COOKIE = "refresh_token";
    private final AuthService authService;
    private final ClientIpResolver clientIpResolver;
    private final AuthProperties properties;

    public AuthController(
            AuthService authService,
            ClientIpResolver clientIpResolver,
            AuthProperties properties
    ) {
        this.authService = authService;
        this.clientIpResolver = clientIpResolver;
        this.properties = properties;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a user account")
    public ApiResponse<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        AuthSession session = authService.register(
                request.email(), request.username(), request.password(), clientIpResolver.resolve(servletRequest)
        );
        setRefreshCookie(servletResponse, session.tokens().refreshToken());
        return success(AuthResponse.from(session));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate a user")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        AuthSession session = authService.login(
                request.email(), request.password(), clientIpResolver.resolve(servletRequest)
        );
        setRefreshCookie(servletResponse, session.tokens().refreshToken());
        return success(AuthResponse.from(session));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate the refresh token")
    public ApiResponse<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        AuthSession session = authService.refresh(refreshToken, clientIpResolver.resolve(servletRequest));
        setRefreshCookie(servletResponse, session.tokens().refreshToken());
        return success(AuthResponse.from(session));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the current refresh token")
    public ApiResponse<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletResponse servletResponse
    ) {
        authService.logout(refreshToken);
        clearRefreshCookie(servletResponse);
        return success(null);
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current user")
    public ApiResponse<UserResponse> me(Authentication authentication) {
        AccessPrincipal principal = (AccessPrincipal) authentication.getPrincipal();
        return success(UserResponse.from(authService.currentUser(principal.userId())));
    }

    @PutMapping("/password")
    @Operation(summary = "Change the current password and revoke all refresh tokens")
    public ApiResponse<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletResponse servletResponse
    ) {
        AccessPrincipal principal = (AccessPrincipal) authentication.getPrincipal();
        authService.changePassword(principal.userId(), request.currentPassword(), request.newPassword());
        clearRefreshCookie(servletResponse);
        return success(null);
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(properties.refreshCookieSecure())
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(properties.refreshTokenTtl())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(properties.refreshCookieSecure())
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private <T> ApiResponse<T> success(T data) {
        return ApiResponse.success(data, RequestIdFilter.currentRequestId());
    }
}
