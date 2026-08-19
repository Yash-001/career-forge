package com.careerforge.backend.billing;

import com.careerforge.backend.auth.domain.SubscriptionTier;

import java.time.Instant;

/**
 * Provider-agnostic view of a subscription returned by a billing provider.
 * No provider SDK types leak into the domain.
 */
public record ProviderSubscriptionState(
        SubscriptionTier tier,
        SubscriptionStatus status,
        String providerCustomerId,
        String providerSubscriptionId,
        Instant currentPeriodStart,
        Instant currentPeriodEnd
) {}
