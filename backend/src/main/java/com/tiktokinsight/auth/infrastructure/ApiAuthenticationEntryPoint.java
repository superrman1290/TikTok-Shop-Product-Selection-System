package com.tiktokinsight.auth.infrastructure;

import com.tiktokinsight.common.api.ApiErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityErrorWriter errorWriter;

    public ApiAuthenticationEntryPoint(SecurityErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        Object failure = request.getAttribute(AccessTokenAuthenticationFilter.AUTH_FAILURE_ATTRIBUTE);
        if (failure == ApiErrorCode.ACCOUNT_DISABLED) {
            errorWriter.write(response, HttpStatus.FORBIDDEN.value(), ApiErrorCode.ACCOUNT_DISABLED);
            return;
        }
        ApiErrorCode code = failure == ApiErrorCode.ACCESS_TOKEN_INVALID
                ? ApiErrorCode.ACCESS_TOKEN_INVALID
                : ApiErrorCode.UNAUTHENTICATED;
        errorWriter.write(response, HttpStatus.UNAUTHORIZED.value(), code);
    }
}
