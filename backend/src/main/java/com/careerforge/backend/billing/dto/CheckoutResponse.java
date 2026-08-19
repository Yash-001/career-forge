package com.careerforge.backend.billing.dto;

import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.billing.SubscriptionStatus;

public record CheckoutResponse(
        String action,
        SubscriptionTier tier,
        SubscriptionStatus status,
        String message
) {}
