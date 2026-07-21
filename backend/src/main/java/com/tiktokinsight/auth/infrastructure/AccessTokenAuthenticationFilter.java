package com.tiktokinsight.auth.infrastructure;

import com.tiktokinsight.auth.application.AccessPrincipal;
import com.tiktokinsight.auth.domain.UserAccount;
import com.tiktokinsight.auth.domain.UserAccountRepository;
import com.tiktokinsight.common.api.ApiErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AccessTokenAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_FAILURE_ATTRIBUTE =
            AccessTokenAuthenticationFilter.class.getName() + ".failure";
    private static final String BEARER_PREFIX = "Bearer ";
    private final TokenService tokenService;
    private final UserAccountRepository userRepository;

    public AccessTokenAuthenticationFilter(TokenService tokenService, UserAccountRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            AccessPrincipal tokenPrincipal = tokenService.decodeAccessToken(
                    authorization.substring(BEARER_PREFIX.length()).trim()
            );
            UserAccount user = userRepository.findById(tokenPrincipal.userId())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown access token subject"));
            if (!user.isEnabled()) {
                SecurityContextHolder.clearContext();
                request.setAttribute(AUTH_FAILURE_ATTRIBUTE, ApiErrorCode.ACCOUNT_DISABLED);
            } else {
                AccessPrincipal principal = new AccessPrincipal(
                        user.id(), user.email(), user.username(), user.role()
                );
                var authentication = UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name()))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            request.setAttribute(AUTH_FAILURE_ATTRIBUTE, ApiErrorCode.ACCESS_TOKEN_INVALID);
        }
        filterChain.doFilter(request, response);
    }
}
