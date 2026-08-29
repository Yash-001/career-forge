package com.careerforge.backend.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Servlet filter that enforces per-IP rate limits on authentication endpoints.
 * Runs before the JWT filter so unauthenticated brute-force attempts are blocked early.
 *
 * SEC-10: Brute-force protection for login, register, and password-reset.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password"
    );

    private final RateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (RATE_LIMITED_PATHS.contains(path)) {
            String ip = resolveClientIp(request);
            String key = path + ":" + ip;

            if (!rateLimitService.isAllowed(key)) {
                log.warn("Rate limit exceeded for path={} ip={}", path, ip);
                response.setStatus(429);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(
                        "{\"status\":429,\"code\":\"RATE_LIMIT_EXCEEDED\"," +
                        "\"message\":\"Too many requests. Please wait before trying again.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Resolves the real client IP, respecting X-Forwarded-For when present.
     * Uses the first (leftmost) address in the chain, which is the original client.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
