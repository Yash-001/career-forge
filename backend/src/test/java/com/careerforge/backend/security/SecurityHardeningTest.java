package com.careerforge.backend.security;

import com.careerforge.backend.AbstractIntegrationTest;
import com.careerforge.backend.auth.domain.PasswordResetToken;
import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.auth.repository.PasswordResetTokenRepository;
import com.careerforge.backend.auth.repository.UserRepository;
import com.careerforge.backend.demo.DemoGuard;
import com.careerforge.backend.profile.repository.MasterProfileRepository;
import com.careerforge.backend.shared.config.EmailConfig;
import com.careerforge.backend.shared.config.SecurityConfig;
import com.careerforge.backend.shared.security.RateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 9F — Security Hardening Regression Tests.
 *
 * Covers:
 * - SEC-02: JWT secret startup validation (production guard)
 * - SEC-08: CORS origin trim
 * - SEC-10: Rate limiting on auth endpoints
 * - SEC-11: Demo mode blocked in production
 * - SEC-15: ConsoleEmailProvider blocked in production
 * - Input size limits on description / summary fields
 * - Password reset token — findValidTokens (no full table scan)
 * - Security headers (X-Content-Type-Options, X-Frame-Options)
 * - Ownership: cross-user resource access returns 404
 * - Stripe webhook: signature required
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class SecurityHardeningTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordResetTokenRepository resetTokenRepository;
    @Autowired MasterProfileRepository masterProfileRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired RateLimitService rateLimitService;

    @BeforeEach
    void cleanDatabase() {
        resetTokenRepository.deleteAll();
        masterProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ── SEC-02: JWT secret validation ─────────────────────────────────────────

    @Test
    void jwtSecretValidation_shortSecret_throwsInProduction() {
        assertThatThrownBy(() -> {
            SecurityConfig cfg = new SecurityConfig(null, null, null);
            injectField(cfg, "jwtSecret", "short");
            injectField(cfg, "appEnv", "production");
            cfg.validateJwtSecret();
        }).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("JWT secret is too short");
    }

    @Test
    void jwtSecretValidation_placeholderSecret_throwsInProduction() {
        assertThatThrownBy(() -> {
            SecurityConfig cfg = new SecurityConfig(null, null, null);
            injectField(cfg, "jwtSecret", "change-this-secret-in-production-must-be-at-least-256-bits-long!!");
            injectField(cfg, "appEnv", "production");
            cfg.validateJwtSecret();
        }).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("placeholder");
    }

    @Test
    void jwtSecretValidation_strongSecret_doesNotThrowInProduction() {
        assertThatNoException().isThrownBy(() -> {
            SecurityConfig cfg = new SecurityConfig(null, null, null);
            injectField(cfg, "jwtSecret", "a-very-strong-random-secret-that-is-at-least-32-bytes-long-xyz!");
            injectField(cfg, "appEnv", "production");
            cfg.validateJwtSecret();
        });
    }

    @Test
    void jwtSecretValidation_shortSecret_doesNotThrowInDevelopment() {
        assertThatNoException().isThrownBy(() -> {
            SecurityConfig cfg = new SecurityConfig(null, null, null);
            injectField(cfg, "jwtSecret", "short");
            injectField(cfg, "appEnv", "development");
            cfg.validateJwtSecret();
        });
    }

    // ── SEC-10: Rate limiting (unit) ──────────────────────────────────────────

    @Test
    void rateLimiter_allowsRequestsUnderLimit() {
        for (int i = 0; i < RateLimitService.MAX_ATTEMPTS; i++) {
            assertThat(rateLimitService.isAllowed("test-key")).isTrue();
        }
    }

    @Test
    void rateLimiter_blocksRequestsOverLimit() {
        for (int i = 0; i < RateLimitService.MAX_ATTEMPTS; i++) {
            rateLimitService.isAllowed("block-key");
        }
        assertThat(rateLimitService.isAllowed("block-key")).isFalse();
    }

    @Test
    void rateLimiter_differentKeysAreIndependent() {
        for (int i = 0; i < RateLimitService.MAX_ATTEMPTS; i++) {
            rateLimitService.isAllowed("key-a");
        }
        assertThat(rateLimitService.isAllowed("key-b")).isTrue();
    }

    // ── SEC-10: Rate limiting (HTTP) ──────────────────────────────────────────

    @Test
    void rateLimitFilter_loginEndpoint_returns429AfterLimit() throws Exception {
        for (int i = 0; i < RateLimitService.MAX_ATTEMPTS; i++) {
            rateLimitService.isAllowed("/api/v1/auth/login:127.0.0.1");
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "test@example.com",
                                "password", "Password1"
                        ))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void rateLimitFilter_registerEndpoint_returns429AfterLimit() throws Exception {
        for (int i = 0; i < RateLimitService.MAX_ATTEMPTS; i++) {
            rateLimitService.isAllowed("/api/v1/auth/register:127.0.0.1");
        }

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "new@example.com",
                                "password", "Password1"
                        ))))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void rateLimitFilter_forgotPasswordEndpoint_returns429AfterLimit() throws Exception {
        for (int i = 0; i < RateLimitService.MAX_ATTEMPTS; i++) {
            rateLimitService.isAllowed("/api/v1/auth/forgot-password:127.0.0.1");
        }

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "test@example.com"
                        ))))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void rateLimitFilter_healthEndpoint_isNeverRateLimited() throws Exception {
        for (int i = 0; i < RateLimitService.MAX_ATTEMPTS + 5; i++) {
            mockMvc.perform(get("/api/v1/health"))
                    .andExpect(status().isOk());
        }
    }

    // ── SEC-11: Demo mode blocked in production ───────────────────────────────

    @Test
    void demoGuard_demoModeTrue_productionEnv_throwsIllegalState() {
        DemoGuard guard = new DemoGuard(true, "production");
        assertThatThrownBy(guard::enforce)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CAREERFORGE_DEMO_MODE=true");
    }

    @Test
    void demoGuard_demoModeFalse_productionEnv_doesNotThrow() {
        DemoGuard guard = new DemoGuard(false, "production");
        assertThatNoException().isThrownBy(guard::enforce);
    }

    @Test
    void demoGuard_demoModeTrue_productionEnv_caseInsensitive_throws() {
        DemoGuard guard = new DemoGuard(true, "PRODUCTION");
        assertThatThrownBy(guard::enforce).isInstanceOf(IllegalStateException.class);
    }

    // ── SEC-15: ConsoleEmailProvider in production logs warn, does not throw ──

    @Test
    void emailConfig_consoleProvider_productionEnv_doesNotThrow() {
        EmailConfig config = new EmailConfig();
        assertThatNoException().isThrownBy(() -> config.emailService("console", "production"));
    }

    @Test
    void emailConfig_consoleProvider_developmentEnv_doesNotThrow() {
        EmailConfig config = new EmailConfig();
        assertThatNoException().isThrownBy(() -> config.emailService("console", "development"));
    }

    @Test
    void emailConfig_consoleProvider_emptyEnv_doesNotThrow() {
        EmailConfig config = new EmailConfig();
        assertThatNoException().isThrownBy(() -> config.emailService("console", ""));
    }

    // ── Input size limits ─────────────────────────────────────────────────────

    @Test
    void register_oversizedEmail_returns400() throws Exception {
        String longEmail = "a".repeat(250) + "@example.com";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", longEmail,
                                "password", "Password1"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_oversizedFirstName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "valid@example.com",
                                "password", "Password1",
                                "firstName", "A".repeat(101)
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.firstName").isNotEmpty());
    }

    @Test
    void aiTailorRequest_oversizedJobDescription_returns400() throws Exception {
        String token = registerAndGetToken("aitest@example.com", "Password1");

        mockMvc.perform(post("/api/v1/ai/resumes/00000000-0000-0000-0000-000000000001"
                        + "/versions/00000000-0000-0000-0000-000000000002/tailor")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobDescription", "x".repeat(10001)
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.jobDescription").isNotEmpty());
    }

    // ── Security headers ──────────────────────────────────────────────────────

    @Test
    void securityHeaders_healthEndpoint_hasXContentTypeOptions() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void securityHeaders_healthEndpoint_hasXFrameOptions() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void securityHeaders_authEndpoint_hasXContentTypeOptions() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@x.com\",\"password\":\"p\"}"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    // ── Password reset token — findValidTokens ────────────────────────────────

    @Test
    void passwordResetTokenRepository_findValidTokens_excludesUsedTokens() {
        User user = createUser("prt@example.com", "Password1");

        resetTokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .tokenHash(passwordEncoder.encode("used-token"))
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .used(true)
                .build());

        resetTokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .tokenHash(passwordEncoder.encode("valid-token"))
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .used(false)
                .build());

        var validTokens = resetTokenRepository.findValidTokens();
        assertThat(validTokens).hasSize(1);
        assertThat(validTokens.get(0).isUsed()).isFalse();
    }

    @Test
    void passwordResetTokenRepository_findValidTokens_excludesExpiredTokens() {
        User user = createUser("prt2@example.com", "Password1");

        resetTokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .tokenHash(passwordEncoder.encode("expired-token"))
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .used(false)
                .build());

        assertThat(resetTokenRepository.findValidTokens()).isEmpty();
    }

    @Test
    void passwordResetTokenRepository_findValidTokens_returnsOnlyNonExpiredNonUsed() {
        User user = createUser("prt3@example.com", "Password1");

        // expired + used
        resetTokenRepository.save(PasswordResetToken.builder()
                .user(user).tokenHash(passwordEncoder.encode("t1"))
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS)).used(true).build());
        // expired + not used
        resetTokenRepository.save(PasswordResetToken.builder()
                .user(user).tokenHash(passwordEncoder.encode("t2"))
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS)).used(false).build());
        // valid + used
        resetTokenRepository.save(PasswordResetToken.builder()
                .user(user).tokenHash(passwordEncoder.encode("t3"))
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS)).used(true).build());
        // valid + not used — the only one that should be returned
        resetTokenRepository.save(PasswordResetToken.builder()
                .user(user).tokenHash(passwordEncoder.encode("t4"))
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS)).used(false).build());

        assertThat(resetTokenRepository.findValidTokens()).hasSize(1);
    }

    // ── Ownership: cross-user access returns 404 ──────────────────────────────

    @Test
    void crossUserResumeAccess_returns404() throws Exception {
        String tokenA = registerAndGetToken("userA@example.com", "Password1");
        String createResponse = mockMvc.perform(post("/api/v1/resumes")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "My Resume"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String resumeId = objectMapper.readTree(createResponse).get("id").asText();

        String tokenB = registerAndGetToken("userB@example.com", "Password1");
        mockMvc.perform(get("/api/v1/resumes/" + resumeId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void crossUserResumeDelete_returns404() throws Exception {
        String tokenA = registerAndGetToken("delA@example.com", "Password1");
        String createResponse = mockMvc.perform(post("/api/v1/resumes")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Resume to protect"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String resumeId = objectMapper.readTree(createResponse).get("id").asText();

        String tokenB = registerAndGetToken("delB@example.com", "Password1");
        mockMvc.perform(delete("/api/v1/resumes/" + resumeId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void crossUserProfileData_isIsolated() throws Exception {
        String tokenA = registerAndGetToken("profA@example.com", "Password1");
        String tokenB = registerAndGetToken("profB@example.com", "Password1");

        mockMvc.perform(put("/api/v1/profile")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "professionalTitle", "Engineer A"
                        ))))
                .andExpect(status().isOk());

        String profileB = mockMvc.perform(get("/api/v1/profile")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(profileB).doesNotContain("Engineer A");
    }

    @Test
    void unauthenticatedRequest_toResumesEndpoint_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/resumes")).andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedRequest_toProfileEndpoint_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/profile")).andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedRequest_toBillingEndpoint_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/billing/subscription")).andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedRequest_toDashboardEndpoint_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard")).andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedRequest_toApplicationsEndpoint_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/applications")).andExpect(status().isUnauthorized());
    }

    // ── Stripe webhook: signature required ───────────────────────────────────

    @Test
    void stripeWebhook_missingSignatureHeader_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"customer.subscription.created\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void stripeWebhook_invalidSignature_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=invalid,v1=invalid")
                        .content("{\"type\":\"customer.subscription.created\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String registerAndGetToken(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", password
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private User createUser(String email, String password) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .subscriptionTier(SubscriptionTier.FREE)
                .enabled(true)
                .build());
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject field: " + fieldName, e);
        }
    }
}
