package com.careerforge.backend.auth.dto;

import com.careerforge.backend.auth.domain.SubscriptionTier;

import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        SubscriptionTier subscriptionTier,
        String accessToken,
        String refreshToken
) {}
