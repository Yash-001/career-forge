package com.careerforge.backend.billing;

import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.auth.domain.User;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Demo billing provider — no external API calls, no payment required.
 * Deterministic behavior for local development and portfolio demonstrations.
 * Registered as a Spring bean; selected by BillingConfig when provider=demo.
 */
@Component
public class DemoBillingProvider implements BillingProviderPort {

    @Override
    public ProviderSubscriptionState initiateUpgrade(User user, Subscription currentSubscription) {
        Instant now = Instant.now();
        return new ProviderSubscriptionState(
                SubscriptionTier.PRO,
                SubscriptionStatus.ACTIVE,
                null,
                null,
                now,
                now.plus(30, ChronoUnit.DAYS)
        );
    }

    @Override
    public ProviderSubscriptionState cancelSubscription(User user, Subscription currentSubscription) {
        return new ProviderSubscriptionState(
                SubscriptionTier.FREE,
                SubscriptionStatus.CANCELED,
                null,
                null,
                currentSubscription.getCurrentPeriodStart(),
                currentSubscription.getCurrentPeriodEnd()
        );
    }

    @Override
    public ProviderSubscriptionState getSubscriptionState(User user, Subscription currentSubscription) {
        return new ProviderSubscriptionState(
                currentSubscription.getTier(),
                currentSubscription.getStatus(),
                currentSubscription.getProviderCustomerId(),
                currentSubscription.getProviderSubscriptionId(),
                currentSubscription.getCurrentPeriodStart(),
                currentSubscription.getCurrentPeriodEnd()
        );
    }

    @Override
    public BillingProvider getProvider() {
        return BillingProvider.DEMO;
    }
}
