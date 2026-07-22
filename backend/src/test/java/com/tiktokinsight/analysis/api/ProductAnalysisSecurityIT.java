package com.tiktokinsight.analysis.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tiktokinsight.analysis.application.AnalysisApplicationService;
import com.tiktokinsight.analysis.application.CostProfileService;
import com.tiktokinsight.auth.domain.UserAccountRepository;
import com.tiktokinsight.auth.infrastructure.AccessTokenAuthenticationFilter;
import com.tiktokinsight.auth.infrastructure.ApiAccessDeniedHandler;
import com.tiktokinsight.auth.infrastructure.ApiAuthenticationEntryPoint;
import com.tiktokinsight.auth.infrastructure.SecurityErrorWriter;
import com.tiktokinsight.auth.infrastructure.TokenService;
import com.tiktokinsight.common.exception.GlobalExceptionHandler;
import com.tiktokinsight.common.logging.RequestIdFilter;
import com.tiktokinsight.common.security.SecurityConfiguration;
import com.tiktokinsight.product.application.ProductCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProductAnalysisController.class, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
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
class ProductAnalysisSecurityIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private AnalysisApplicationService analysisService;
    @MockitoBean private CostProfileService costProfileService;
    @MockitoBean private ProductCatalogService productCatalogService;
    @MockitoBean private TokenService tokenService;
    @MockitoBean private UserAccountRepository userRepository;

    @Test
    void rejectsAnonymousAnalysisAndCostRequests() throws Exception {
        mockMvc.perform(get("/api/v1/products/1/analysis"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
        mockMvc.perform(get("/api/v1/products/1/cost-profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }
}
