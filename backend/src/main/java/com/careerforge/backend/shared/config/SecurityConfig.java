package com.careerforge.backend.shared.config;

import com.careerforge.backend.shared.security.JwtAuthenticationFilter;
import com.careerforge.backend.shared.security.RateLimitFilter;
import com.careerforge.backend.shared.security.UserDetailsServiceImpl;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Central Spring Security configuration.
 *
 * Security properties enforced here:
 * - SEC-02: JWT secret minimum length validated at startup (production guard).
 * - SEC-08: CORS restricted to configured origins only (trimmed to prevent whitespace bypass).
 * - SEC-10: RateLimitFilter registered before JWT filter.
 * - SEC-01: Security headers (X-Content-Type-Options, X-Frame-Options, Referrer-Policy).
 * - CSRF disabled — stateless JWT API; no session cookies.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final int JWT_SECRET_MIN_BYTES = 32; // 256 bits

    /** Known insecure placeholder values that must never be used in production. */
    private static final List<String> INSECURE_SECRET_PREFIXES = List.of(
            "change-this",
            "replace-this",
            "your-secret",
            "secret",
            "test-secret"
    );

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.env:}")
    private String appEnv;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RateLimitFilter rateLimitFilter,
                          UserDetailsServiceImpl userDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.userDetailsService = userDetailsService;
    }

    /**
     * SEC-02: Validates the JWT secret at startup.
     * In production, rejects weak or placeholder secrets.
     * In non-production environments, logs a warning but allows startup to continue.
     */
    @PostConstruct
    public void validateJwtSecret() {
        boolean isProduction = "production".equalsIgnoreCase(appEnv);

        if (jwtSecret == null || jwtSecret.getBytes().length < JWT_SECRET_MIN_BYTES) {
            String msg = "[SECURITY] JWT secret is too short (minimum 32 bytes / 256 bits required). " +
                         "Generate with: openssl rand -base64 64";
            if (isProduction) {
                throw new IllegalStateException(msg);
            }
            // Log as error in non-production so it's visible but doesn't block dev startup
            org.slf4j.LoggerFactory.getLogger(SecurityConfig.class).error(msg);
        }

        boolean isInsecure = INSECURE_SECRET_PREFIXES.stream()
                .anyMatch(prefix -> jwtSecret.toLowerCase().startsWith(prefix));
        if (isInsecure) {
            String msg = "[SECURITY] JWT secret appears to be a placeholder value. " +
                         "Set a cryptographically random secret via JWT_SECRET environment variable.";
            if (isProduction) {
                throw new IllegalStateException(msg);
            }
            org.slf4j.LoggerFactory.getLogger(SecurityConfig.class).warn(msg);
        }
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers
                // SEC-01: Prevent MIME-type sniffing
                .contentTypeOptions(contentTypeOptions -> {})
                // SEC-01: Prevent clickjacking
                .frameOptions(frameOptions -> frameOptions.deny())
                // SEC-01: Referrer policy — don't leak URL to third parties
                .referrerPolicy(referrer ->
                    referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/v1/auth/**",
                    "/api/v1/health",
                    "/api/v1/webhooks/stripe",
                    "/api/v1/demo/login",
                    "/actuator/health"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedEntryPoint()))
            .authenticationProvider(authenticationProvider())
            // Rate limit filter runs before JWT filter (SEC-10)
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // SEC-08: Trim each origin to prevent whitespace bypass (e.g. " http://evil.com")
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) ->
                response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
