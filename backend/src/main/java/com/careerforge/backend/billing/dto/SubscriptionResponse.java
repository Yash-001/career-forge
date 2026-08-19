package com.careerforge.backend.billing.dto;

import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.billing.BillingProvider;
import com.careerforge.backend.billing.SubscriptionStatus;

import java.time.Instant;

public record SubscriptionResponse(
        SubscriptionTier tier,
        SubscriptionStatus status,
        BillingProvider provider,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        Integer pdfExportsUsed,
        Integer pdfExportsLimit
) {}
