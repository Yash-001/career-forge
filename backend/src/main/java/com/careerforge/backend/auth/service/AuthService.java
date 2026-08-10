package com.careerforge.backend.auth.service;

import com.careerforge.backend.auth.domain.PasswordResetToken;
import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.auth.dto.*;
import com.careerforge.backend.auth.repository.PasswordResetTokenRepository;
import com.careerforge.backend.auth.repository.UserRepository;
import com.careerforge.backend.shared.email.EmailService;
import com.careerforge.backend.shared.exception.DomainExceptions;
import com.careerforge.backend.shared.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int RESET_TOKEN_EXPIRY_HOURS = 1;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    // ── Registration ──────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw DomainExceptions.emailAlreadyExists();
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .subscriptionTier(SubscriptionTier.FREE)
                .enabled(true)
                .build();

        user = userRepository.save(user);

        log.info("event=USER_REGISTERED userId={}", user.getId());

        return buildAuthResponse(user);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (BadCredentialsException ex) {
            throw DomainExceptions.invalidCredentials();
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(DomainExceptions::invalidCredentials);

        log.info("event=USER_LOGIN userId={}", user.getId());

        return buildAuthResponse(user);
    }

    // ── Forgot Password ───────────────────────────────────────────────────────

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Always return success to prevent email enumeration (ERR-07)
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            resetTokenRepository.deleteAllByUser(user);

            String rawToken = generateSecureToken();
            String tokenHash = passwordEncoder.encode(rawToken);

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(tokenHash)
                    .expiresAt(Instant.now().plus(RESET_TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS))
                    .used(false)
                    .build();

            resetTokenRepository.save(resetToken);

            String resetLink = frontendUrl + "/reset-password?token=" + rawToken;

            try {
                emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
            } catch (Exception ex) {
                log.error("event=EMAIL_DELIVERY_FAILED userId={}", user.getId(), ex);
            }

            log.info("event=PASSWORD_RESET_REQUESTED userId={}", user.getId());
        });
    }

    // ── Refresh Token ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public RefreshResponse refresh(RefreshTokenRequest request) {
        String token = request.refreshToken();

        if (!jwtService.isTokenValid(token) || !jwtService.isRefreshToken(token)) {
            throw DomainExceptions.invalidRefreshToken();
        }

        UUID userId;
        try {
            userId = UUID.fromString(jwtService.extractUserId(token));
        } catch (IllegalArgumentException ex) {
            throw DomainExceptions.invalidRefreshToken();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(DomainExceptions::invalidRefreshToken);

        if (!user.isEnabled()) {
            throw DomainExceptions.invalidRefreshToken();
        }

        return new RefreshResponse(jwtService.generateAccessToken(user.getId(), user.getEmail()));
    }

    // ── Reset Password ────────────────────────────────────────────────────────

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // Find all non-used tokens and check each against the raw token
        PasswordResetToken resetToken = resetTokenRepository.findAll().stream()
                .filter(t -> !t.isUsed() && passwordEncoder.matches(request.token(), t.getTokenHash()))
                .findFirst()
                .orElseThrow(DomainExceptions::invalidOrExpiredToken);

        if (resetToken.isExpired()) {
            throw DomainExceptions.invalidOrExpiredToken();
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);

        log.info("event=PASSWORD_RESET_COMPLETED userId={}", user.getId());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());
        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getSubscriptionTier(),
                accessToken,
                refreshToken
        );
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
