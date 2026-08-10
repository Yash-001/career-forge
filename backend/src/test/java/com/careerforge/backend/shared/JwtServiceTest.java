package com.careerforge.backend.shared;

import com.careerforge.backend.shared.security.JwtService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    // 64-char secret = 512 bits — well above the 256-bit minimum
    private final JwtService jwtService = new JwtService(
            "test-secret-that-is-long-enough-for-hs256-algorithm-padding!!",
            900_000L,
            604_800_000L
    );

    @Test
    void generateAccessToken_isValid() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId, "user@example.com");
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void extractEmail_returnsCorrectEmail() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId, "user@example.com");
        assertThat(jwtService.extractEmail(token)).isEqualTo("user@example.com");
    }

    @Test
    void extractUserId_returnsCorrectId() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId, "user@example.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId.toString());
    }

    @Test
    void invalidToken_isNotValid() {
        assertThat(jwtService.isTokenValid("not.a.valid.token")).isFalse();
    }

    @Test
    void tamperedToken_isNotValid() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId, "user@example.com");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }
}
