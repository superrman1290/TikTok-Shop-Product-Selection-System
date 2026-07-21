package com.tiktokinsight;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tiktokinsight.common.api.health.ComponentStatus;
import com.tiktokinsight.common.api.health.HealthController;
import com.tiktokinsight.common.api.health.SystemHealth;
import com.tiktokinsight.common.api.health.SystemHealthService;
import com.tiktokinsight.common.logging.RequestIdFilter;
import com.tiktokinsight.common.security.SecurityConfiguration;
import com.tiktokinsight.auth.infrastructure.AccessTokenAuthenticationFilter;
import com.tiktokinsight.auth.infrastructure.ApiAccessDeniedHandler;
import com.tiktokinsight.auth.infrastructure.ApiAuthenticationEntryPoint;
import com.tiktokinsight.auth.infrastructure.SecurityErrorWriter;
import com.tiktokinsight.auth.infrastructure.TokenService;
import com.tiktokinsight.auth.domain.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = HealthController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@AutoConfigureMockMvc
@Import({
        RequestIdFilter.class,
        SecurityConfiguration.class,
        AccessTokenAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        SecurityErrorWriter.class
})
class HealthEndpointIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SystemHealthService systemHealthService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UserAccountRepository userAccountRepository;

    @BeforeEach
    void setUp() {
        when(systemHealthService.check()).thenReturn(new SystemHealth(
                ComponentStatus.UP,
                ComponentStatus.UP,
                ComponentStatus.UP,
                ComponentStatus.UP
        ));
    }

    @Test
    void returnsUnifiedOperationalHealthResponse() throws Exception {
        mockMvc.perform(get("/api/v1/health").header("X-Request-Id", "integration-test-20260721"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "integration-test-20260721"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.requestId").value("integration-test-20260721"))
                .andExpect(jsonPath("$.data.application").value("UP"))
                .andExpect(jsonPath("$.data.database").value("UP"))
                .andExpect(jsonPath("$.data.redis").value("UP"))
                .andExpect(jsonPath("$.data.storage").value("UP"))
                .andExpect(jsonPath("$.data.operational").doesNotExist());
    }
}
