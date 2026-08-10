package com.careerforge.backend.auth.controller;

import com.careerforge.backend.AbstractIntegrationTest;
import com.careerforge.backend.auth.domain.PasswordResetToken;
import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.auth.repository.PasswordResetTokenRepository;
import com.careerforge.backend.auth.repository.UserRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordResetTokenRepository resetTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        resetTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ── Health endpoint ───────────────────────────────────────────────────────

    @Test
    void healthEndpoint_isPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    // ── Registration ──────────────────────────────────────────────────────────

    @Test
    void register_success_returns201WithTokens() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "jane@example.com",
                                "password", "Password1",
                                "firstName", "Jane",
                                "lastName", "Doe"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.subscriptionTier").value("FREE"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.userId").isNotEmpty());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        createUser("dup@example.com", "Password1");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "dup@example.com",
                                "password", "Password1"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void register_invalidEmail_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "not-an-email",
                                "password", "Password1"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.email").isNotEmpty());
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "user@example.com",
                                "password", "short"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").isNotEmpty());
    }

    @Test
    void register_passwordWithoutNumber_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "user@example.com",
                                "password", "NoNumberHere"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").isNotEmpty());
    }

    @Test
    void register_passwordIsNotStoredInPlaintext() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "hash@example.com",
                                "password", "Password1"
                        ))))
                .andExpect(status().isCreated());

        User saved = userRepository.findByEmail("hash@example.com").orElseThrow();
        assertThat(saved.getPasswordHash()).doesNotContain("Password1");
        assertThat(saved.getPasswordHash()).startsWith("$2a$");
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    void login_success_returns200WithTokens() throws Exception {
        createUser("login@example.com", "Password1");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "login@example.com",
                                "password", "Password1"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        createUser("login@example.com", "Password1");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "login@example.com",
                                "password", "WrongPass1"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void login_unknownEmail_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "nobody@example.com",
                                "password", "Password1"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    // ── Forgot Password ───────────────────────────────────────────────────────

    @Test
    void forgotPassword_existingEmail_returns200AndCreatesToken() throws Exception {
        createUser("reset@example.com", "Password1");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "reset@example.com"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        assertThat(resetTokenRepository.findAll()).hasSize(1);
    }

    @Test
    void forgotPassword_unknownEmail_returns200WithoutRevealingExistence() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "ghost@example.com"
                        ))))
                .andExpect(status().isOk());
    }

    // ── Reset Password ────────────────────────────────────────────────────────

    @Test
    void resetPassword_validToken_returns200AndUpdatesPassword() throws Exception {
        User user = createUser("reset2@example.com", "OldPass1");
        String rawToken = "valid-raw-token-abc123";
        createResetToken(user, rawToken, Instant.now().plus(1, ChronoUnit.HOURS), false);

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "token", rawToken,
                                "newPassword", "NewPass1"
                        ))))
                .andExpect(status().isOk());

        User updated = userRepository.findByEmail("reset2@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("NewPass1", updated.getPasswordHash())).isTrue();
    }

    @Test
    void resetPassword_expiredToken_returns400() throws Exception {
        User user = createUser("expired@example.com", "OldPass1");
        String rawToken = "expired-token-abc123";
        createResetToken(user, rawToken, Instant.now().minus(1, ChronoUnit.HOURS), false);

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "token", rawToken,
                                "newPassword", "NewPass1"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OR_EXPIRED_TOKEN"));
    }

    @Test
    void resetPassword_alreadyUsedToken_returns400() throws Exception {
        User user = createUser("used@example.com", "OldPass1");
        String rawToken = "used-token-abc123";
        createResetToken(user, rawToken, Instant.now().plus(1, ChronoUnit.HOURS), true);

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "token", rawToken,
                                "newPassword", "NewPass1"
                        ))))
                .andExpect(status().isBadRequest());
    }

    // ── Security ──────────────────────────────────────────────────────────────

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/protected-does-not-exist"))
                .andExpect(status().isUnauthorized());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User createUser(String email, String password) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .subscriptionTier(SubscriptionTier.FREE)
                .enabled(true)
                .build());
    }

    private void createResetToken(User user, String rawToken, Instant expiresAt, boolean used) {
        resetTokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .tokenHash(passwordEncoder.encode(rawToken))
                .expiresAt(expiresAt)
                .used(used)
                .build());
    }
}
