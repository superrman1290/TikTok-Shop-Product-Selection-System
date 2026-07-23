package com.tiktokinsight.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiktokinsight.common.api.ApiErrorCode;
import com.tiktokinsight.common.api.ApiResponse;
import com.tiktokinsight.common.logging.RequestIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiRateLimitFilter extends OncePerRequestFilter {
    private final StringRedisTemplate redis; private final ObjectMapper json; private final int limit;
    public ApiRateLimitFilter(ObjectProvider<StringRedisTemplate> redis, ObjectMapper json, @Value("${app.security.api-rate-limit-per-minute:120}") int limit) { this.redis=redis.getIfAvailable();this.json=json;this.limit=limit; }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) { return !request.getRequestURI().startsWith("/api/") || request.getRequestURI().equals("/api/v1/health"); }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        try { if (redis != null) { String key="rate:"+client(request)+":"+(System.currentTimeMillis()/60000); Long count=redis.opsForValue().increment(key); if(count!=null&&count==1)redis.expire(key,Duration.ofMinutes(2)); if(count!=null&&count>limit){response.setStatus(429);response.setContentType("application/json");json.writeValue(response.getOutputStream(),ApiResponse.failure(ApiErrorCode.API_RATE_LIMITED.code(),ApiErrorCode.API_RATE_LIMITED.message(),null,RequestIdFilter.currentRequestId()));return;} } } catch (RuntimeException ignored) { }
        chain.doFilter(request,response);
    }
    private String client(HttpServletRequest request) { String forwarded=request.getHeader("X-Forwarded-For"); return forwarded==null||forwarded.isBlank()?request.getRemoteAddr():forwarded.split(",")[0].trim(); }
}
