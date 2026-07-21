package com.tiktokinsight.importing.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tiktokinsight.auth.application.AccessPrincipal;
import com.tiktokinsight.auth.domain.UserAccount;
import com.tiktokinsight.auth.domain.UserAccountRepository;
import com.tiktokinsight.auth.domain.UserRole;
import com.tiktokinsight.auth.domain.UserStatus;
import com.tiktokinsight.auth.infrastructure.AccessTokenAuthenticationFilter;
import com.tiktokinsight.auth.infrastructure.ApiAccessDeniedHandler;
import com.tiktokinsight.auth.infrastructure.ApiAuthenticationEntryPoint;
import com.tiktokinsight.auth.infrastructure.SecurityErrorWriter;
import com.tiktokinsight.auth.infrastructure.TokenService;
import com.tiktokinsight.common.api.PageResponse;
import com.tiktokinsight.common.exception.GlobalExceptionHandler;
import com.tiktokinsight.common.logging.RequestIdFilter;
import com.tiktokinsight.common.security.SecurityConfiguration;
import com.tiktokinsight.importing.application.ImportApplicationService;
import com.tiktokinsight.product.api.ProductController;
import com.tiktokinsight.product.application.ProductCatalogService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = {ImportAdministrationController.class, ProductController.class},
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
class ProductDataSecurityIT {

    private static final Instant NOW = Instant.parse("2026-07-21T08:30:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImportApplicationService importService;

    @MockitoBean
    private ProductCatalogService productService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UserAccountRepository userRepository;

    @Test
    void allowsAuthenticatedUsersToBrowseButOnlyAdminsToImport() throws Exception {
        stubUser(UserRole.USER);
        when(productService.list(any())).thenReturn(new PageResponse<>(1, 20, 0, List.of()));

        mockMvc.perform(get("/api/v1/products").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        MockMultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv", "header".getBytes());
        mockMvc.perform(multipart("/api/v1/admin/imports/products")
                        .file(file)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));

        stubUser(UserRole.ADMIN);
        mockMvc.perform(multipart("/api/v1/admin/imports/products")
                        .file(file)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsAnonymousProductAccess() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    private void stubUser(UserRole role) {
        AccessPrincipal principal = new AccessPrincipal(1, "user@example.com", "User", role);
        when(tokenService.decodeAccessToken(anyString())).thenReturn(principal);
        when(userRepository.findById(1)).thenReturn(Optional.of(new UserAccount(
                1L, "user@example.com", "User", "hash", role, UserStatus.ENABLED,
                null, NOW, NOW, NOW
        )));
    }
}
