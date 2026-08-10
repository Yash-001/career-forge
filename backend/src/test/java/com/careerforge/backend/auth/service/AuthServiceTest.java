package com.careerforge.backend.auth.service;

import com.careerforge.backend.auth.domain.PasswordResetToken;
import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.auth.dto.*;
import com.careerforge.backend.auth.repository.PasswordResetTokenRepository;
import com.careerforge.backend.auth.repository.UserRepository;
import com.careerforge.backend.shared.email.EmailService;
import com.careerforge.backend.shared.exception.ApiException;
import com.careerforge.backend.shared.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordResetTokenRepository resetTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;
    @Mock EmailService emailService;

    @InjectMocks AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "frontendUrl", "http://localhost:5173");
    }

    // ── Registration ──────────────────────────────────────────────────────────

    @Test
    void register_success_returnsAuthResponse() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("hashed");
        when(jwtService.generateAccessToken(any(), any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(), any())).thenReturn("refresh-token");

        User saved = User.builder()
                .id(UUID.randomUUID()).email("user@example.com")
                .passwordHash("hashed").subscriptionTier(SubscriptionTier.FREE).build();
        when(userRepository.save(any())).thenReturn(saved);

        AuthResponse response = authService.register(
                new RegisterRequest("user@example.com", "Password1", "Jane", "Doe"));

        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.subscriptionTier()).isEqualTo(SubscriptionTier.FREE);
        assertThat(response.accessToken()).isEqualTo("access-token");
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("dup@example.com", "Password1", null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void register_passwordIsHashed_notStoredInPlaintext() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("bcrypt-hash");
        when(jwtService.generateAccessToken(any(), any())).thenReturn("t");
        when(jwtService.generateRefreshToken(any(), any())).thenReturn("r");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        User saved = User.builder().id(UUID.randomUUID()).email("a@b.com")
                .passwordHash("bcrypt-hash").subscriptionTier(SubscriptionTier.FREE).build();
        when(userRepository.save(captor.capture())).thenReturn(saved);

        authService.register(new RegisterRequest("a@b.com", "Password1", null, null));

        assertThat(captor.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(captor.getValue().getPasswordHash()).doesNotContain("Password1");
    }

    @Test
    void register_defaultSubscriptionIsFree() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(jwtService.generateAccessToken(any(), any())).thenReturn("t");
        when(jwtService.generateRefreshToken(any(), any())).thenReturn("r");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        User saved = User.builder().id(UUID.randomUUID()).email("a@b.com")
                .passwordHash("hash").subscriptionTier(SubscriptionTier.FREE).build();
        when(userRepository.save(captor.capture())).thenReturn(saved);

        AuthResponse response = authService.register(
                new RegisterRequest("a@b.com", "Password1", null, null));

        assertThat(response.subscriptionTier()).isEqualTo(SubscriptionTier.FREE);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    void login_success_returnsAuthResponse() {
        User user = User.builder().id(UUID.randomUUID()).email("user@example.com")
                .passwordHash("hash").subscriptionTier(SubscriptionTier.FREE).build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(any(), any())).thenReturn("access");
        when(jwtService.generateRefreshToken(any(), any())).thenReturn("refresh");

        AuthResponse response = authService.login(
                new LoginRequest("user@example.com", "Password1"));

        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.accessToken()).isEqualTo("access");
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        doThrow(new BadCredentialsException("bad"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("user@example.com", "WrongPass1")))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void login_unknownEmail_throwsUnauthorized() {
        doThrow(new BadCredentialsException("bad"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("nobody@example.com", "Password1")))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("INVALID_CREDENTIALS");
    }

    // ── Forgot Password ───────────────────────────────────────────────────────

    @Test
    void forgotPassword_existingUser_generatesTokenAndCallsEmailService() {
        User user = User.builder().id(UUID.randomUUID()).email("user@example.com")
                .passwordHash("hash").subscriptionTier(SubscriptionTier.FREE).build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(any())).thenReturn("token-hash");
        when(resetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        authService.forgotPassword(new ForgotPasswordRequest("user@example.com"));

        verify(resetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq("user@example.com"), contains("reset-password?token="));
    }

    @Test
    void forgotPassword_unknownEmail_doesNotRevealExistence() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        // Must not throw — silent success
        assertThatCode(() -> authService.forgotPassword(
                new ForgotPasswordRequest("ghost@example.com")))
                .doesNotThrowAnyException();

        verify(emailService, never()).sendPasswordResetEmail(any(), any());
    }

    // ── Reset Password ────────────────────────────────────────────────────────

    @Test
    void resetPassword_validToken_updatesPasswordAndMarksUsed() {
        User user = User.builder().id(UUID.randomUUID()).email("user@example.com")
                .passwordHash("old-hash").subscriptionTier(SubscriptionTier.FREE).build();

        PasswordResetToken token = PasswordResetToken.builder()
                .id(UUID.randomUUID()).user(user).tokenHash("token-hash")
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS)).used(false).build();

        when(resetTokenRepository.findAll()).thenReturn(List.of(token));
        when(passwordEncoder.matches("raw-token", "token-hash")).thenReturn(true);
        when(passwordEncoder.encode("NewPass1")).thenReturn("new-hash");

        authService.resetPassword(new ResetPasswordRequest("raw-token", "NewPass1"));

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(token.isUsed()).isTrue();
    }

    @Test
    void resetPassword_expiredToken_throwsBadRequest() {
        User user = User.builder().id(UUID.randomUUID()).email("user@example.com")
                .passwordHash("hash").subscriptionTier(SubscriptionTier.FREE).build();

        PasswordResetToken token = PasswordResetToken.builder()
                .id(UUID.randomUUID()).user(user).tokenHash("token-hash")
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS)).used(false).build();

        when(resetTokenRepository.findAll()).thenReturn(List.of(token));
        when(passwordEncoder.matches("raw-token", "token-hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.resetPassword(
                new ResetPasswordRequest("raw-token", "NewPass1")))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("INVALID_OR_EXPIRED_TOKEN");
    }

    @Test
    void resetPassword_alreadyUsedToken_throwsBadRequest() {
        when(resetTokenRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> authService.resetPassword(
                new ResetPasswordRequest("used-token", "NewPass1")))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("INVALID_OR_EXPIRED_TOKEN");
    }
}
