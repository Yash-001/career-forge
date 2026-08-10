package com.careerforge.backend.auth.controller;

import com.careerforge.backend.AbstractIntegrationTest;
import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.auth.repository.PasswordResetTokenRepository;
import com.careerforge.backend.auth.repository.UserRepository;
import com.careerforge.backend.shared.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class RefreshTokenIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordResetTokenRepository resetTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    @Value("${app.jwt.secret}")
    String jwtSecret;

    @BeforeEach
    void cleanDatabase() {
        resetTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ── Config ────────────────────────────────────────────────────────────────

    @Test
    void jwtSecret_isLoadedFromEnvironmentConfiguration() {
        assertThat(jwtSecret).isNotBlank();
        assertThat(jwtSecret).doesNotContain("null");
    }

    // ── Refresh Token ─────────────────────────────────────────────────────────

    @Test
    void refresh_validRefreshToken_returnsNewAccessToken() throws Exception {
        User user = createUser("refresh@example.com", "Password1", true);
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void refresh_accessTokenUsedAsRefreshToken_returns401() throws Exception {
        User user = createUser("access@example.com", "Password1", true);
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", accessToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void refresh_malformedToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", "not.a.valid.token"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void refresh_expiredRefreshToken_returns401() throws Exception {
        // Build a JwtService with 1ms refresh expiry to produce an already-expired token
        JwtService shortLived = new JwtService(jwtSecret, 900_000L, 1L);
        User user = createUser("expired@example.com", "Password1", true);
        String expiredToken = shortLived.generateRefreshToken(user.getId(), user.getEmail());

        // Small sleep to ensure expiry
        Thread.sleep(10);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", expiredToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void refresh_nonexistentUser_returns401() throws Exception {
        // Generate a token for a UUID that has no corresponding user in the DB
        UUID ghostId = UUID.randomUUID();
        String refreshToken = jwtService.generateRefreshToken(ghostId, "ghost@example.com");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void refresh_disabledUser_returns401() throws Exception {
        User user = createUser("disabled@example.com", "Password1", false);
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private User createUser(String email, String password, boolean enabled) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .subscriptionTier(SubscriptionTier.FREE)
                .enabled(enabled)
                .build());
    }
}
