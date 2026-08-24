package com.careerforge.backend.dashboard.dto;

import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.billing.BillingProvider;
import com.careerforge.backend.billing.SubscriptionStatus;

import java.time.Instant;

public record SubscriptionSummary(
        SubscriptionTier tier,
        SubscriptionStatus status,
        BillingProvider provider,
        Instant currentPeriodStart,
        Instant currentPeriodEnd
) {}
