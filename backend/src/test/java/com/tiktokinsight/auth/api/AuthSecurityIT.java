package com.tiktokinsight.auth.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tiktokinsight.auth.application.AccessPrincipal;
import com.tiktokinsight.auth.application.AccountAdministrationService;
import com.tiktokinsight.auth.application.AuthService;
import com.tiktokinsight.auth.application.AuthSession;
import com.tiktokinsight.auth.application.IssuedTokens;
import com.tiktokinsight.auth.domain.UserAccount;
import com.tiktokinsight.auth.domain.UserAccountRepository;
import com.tiktokinsight.auth.domain.UserRole;
import com.tiktokinsight.auth.domain.UserStatus;
import com.tiktokinsight.auth.infrastructure.AccessTokenAuthenticationFilter;
import com.tiktokinsight.auth.infrastructure.ApiAccessDeniedHandler;
import com.tiktokinsight.auth.infrastructure.ApiAuthenticationEntryPoint;
import com.tiktokinsight.auth.infrastructure.AuthProperties;
import com.tiktokinsight.auth.infrastructure.SecurityErrorWriter;
import com.tiktokinsight.auth.infrastructure.TokenService;
import com.tiktokinsight.common.exception.GlobalExceptionHandler;
import com.tiktokinsight.common.logging.RequestIdFilter;
import com.tiktokinsight.common.security.SecurityConfiguration;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = {AuthController.class, AccountAdministrationController.class},
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@AutoConfigureMockMvc
@Import({
        RequestIdFilter.class,
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        AccessTokenAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        SecurityErrorWriter.class
})
class AuthSecurityIT {

    private static final Instant NOW = Instant.parse("2026-07-21T08:30:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AccountAdministrationService administrationService;

    @MockitoBean
    private ClientIpResolver clientIpResolver;

    @MockitoBean
    private AuthProperties authProperties;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UserAccountRepository userRepository;

    @BeforeEach
    void setUp() {
        when(clientIpResolver.resolve(org.mockito.ArgumentMatchers.any())).thenReturn("203.0.113.5");
        when(authProperties.refreshCookieSecure()).thenReturn(false);
        when(authProperties.refreshTokenTtl()).thenReturn(Duration.ofDays(30));
    }

    @Test
    void registersAndSetsHttpOnlyRefreshCookie() throws Exception {
        AccessPrincipal user = new AccessPrincipal(1, "buyer@example.com", "Buyer", UserRole.USER);
        IssuedTokens tokens = new IssuedTokens("access-token", "refresh-token", NOW.plusSeconds(7200));
        when(authService.register("buyer@example.com", "Buyer", "Password1", "203.0.113.5"))
                .thenReturn(new AuthSession(tokens, user));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"email":"buyer@example.com","username":"Buyer","password":"Password1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly(AuthController.REFRESH_COOKIE, true))
                .andExpect(cookie().value(AuthController.REFRESH_COOKIE, "refresh-token"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.user.role").value("USER"));
    }

    @Test
    void rejectsAnonymousAndInvalidAccessTokensWithFixedCodes() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));

        when(tokenService.decodeAccessToken("bad-token")).thenThrow(new JwtException("invalid"));
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40102));
    }

    @Test
    void preventsNormalUserFromCallingAdminApi() throws Exception {
        stubAuthenticatedUser(UserRole.USER, UserStatus.ENABLED);

        mockMvc.perform(put("/api/v1/admin/users/2/status")
                        .header("Authorization", "Bearer valid-token")
                        .contentType("application/json")
                        .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void allowsAdministratorAndRejectsDisabledBearerUser() throws Exception {
        stubAuthenticatedUser(UserRole.ADMIN, UserStatus.ENABLED);
        AccessPrincipal updated = new AccessPrincipal(2, "buyer@example.com", "Buyer", UserRole.USER);
        when(administrationService.updateStatus(2, UserStatus.DISABLED)).thenReturn(updated);

        mockMvc.perform(put("/api/v1/admin/users/2/status")
                        .header("Authorization", "Bearer valid-token")
                        .contentType("application/json")
                        .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(2));

        stubAuthenticatedUser(UserRole.USER, UserStatus.DISABLED);
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40302));
    }

    @Test
    void validatesPasswordContractWithoutCallingService() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"email":"invalid","username":"x","password":"lettersOnly"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    private void stubAuthenticatedUser(UserRole role, UserStatus status) {
        AccessPrincipal tokenPrincipal = new AccessPrincipal(1, "user@example.com", "User", role);
        when(tokenService.decodeAccessToken(anyString())).thenReturn(tokenPrincipal);
        when(userRepository.findById(1)).thenReturn(Optional.of(new UserAccount(
                1L, "user@example.com", "User", "hash", role, status, null, NOW, NOW, NOW
        )));
    }
}
