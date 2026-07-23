package com.tiktokinsight.common.security;

import com.tiktokinsight.auth.infrastructure.AccessTokenAuthenticationFilter;
import com.tiktokinsight.auth.infrastructure.ApiAccessDeniedHandler;
import com.tiktokinsight.auth.infrastructure.ApiAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

@Configuration
public class SecurityConfiguration {

    // Request IDs are a servlet concern; rate limiting follows token authentication in this chain.

    private final AccessTokenAuthenticationFilter accessTokenFilter;
    private final ApiAuthenticationEntryPoint authenticationEntryPoint;
    private final ApiAccessDeniedHandler accessDeniedHandler;
    private final ApiRateLimitFilter apiRateLimitFilter;

    public SecurityConfiguration(
            AccessTokenAuthenticationFilter accessTokenFilter,
            ApiAuthenticationEntryPoint authenticationEntryPoint,
            ApiAccessDeniedHandler accessDeniedHandler,
            ApiRateLimitFilter apiRateLimitFilter
    ) {
        this.accessTokenFilter = accessTokenFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.apiRateLimitFilter = apiRateLimitFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .contentTypeOptions(content -> {})
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(policy -> policy.policy(ReferrerPolicy.NO_REFERRER))
                        .permissionsPolicyHeader(policy -> policy.policy("camera=(), microphone=(), geolocation=()"))
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/health",
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout",
                                "/actuator/health",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/error"
                        ).permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(accessTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(apiRateLimitFilter, AccessTokenAuthenticationFilter.class)
                .build();
    }
}
