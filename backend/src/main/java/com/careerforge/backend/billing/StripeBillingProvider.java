package com.careerforge.backend.billing;

import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.shared.exception.DomainExceptions;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerSearchResult;
import com.stripe.model.Subscription;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerSearchParams;
import com.stripe.param.SubscriptionCancelParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionRetrieveParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Stripe test-mode billing provider.
 * All Stripe SDK types are confined to this class — nothing leaks into the domain.
 * Selected by BillingConfig when app.billing.provider=stripe.
 * Validates credentials at call time; the application starts without them.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StripeBillingProvider implements BillingProviderPort {

    private final StripeProperties stripeProperties;

    // ── BillingProviderPort ───────────────────────────────────────────────────

    @Override
    public ProviderSubscriptionState initiateUpgrade(User user,
            com.careerforge.backend.billing.Subscription currentSubscription) {
        requireCredentials();
        configureStripe();

        try {
            String customerId = resolveOrCreateCustomer(user, currentSubscription);
            Subscription stripeSub = createSubscription(customerId);
            return toState(stripeSub, customerId);
        } catch (StripeException e) {
            log.error("Stripe upgrade failed for user {}: {}", user.getId(), e.getMessage());
            throw DomainExceptions.billingProviderError(e.getMessage());
        }
    }

    @Override
    public ProviderSubscriptionState cancelSubscription(User user,
            com.careerforge.backend.billing.Subscription currentSubscription) {
        requireCredentials();
        configureStripe();

        String stripeSubId = currentSubscription.getProviderSubscriptionId();
        if (stripeSubId == null || stripeSubId.isBlank()) {
            throw DomainExceptions.billingProviderError("No Stripe subscription ID on record to cancel.");
        }

        try {
            Subscription stripeSub = Subscription.retrieve(stripeSubId);
            Subscription canceled = stripeSub.cancel(
                    SubscriptionCancelParams.builder().build());
            return toState(canceled, currentSubscription.getProviderCustomerId());
        } catch (StripeException e) {
            log.error("Stripe cancel failed for user {}: {}", user.getId(), e.getMessage());
            throw DomainExceptions.billingProviderError(e.getMessage());
        }
    }

    @Override
    public ProviderSubscriptionState getSubscriptionState(User user,
            com.careerforge.backend.billing.Subscription currentSubscription) {
        requireCredentials();
        configureStripe();

        String stripeSubId = currentSubscription.getProviderSubscriptionId();
        if (stripeSubId == null || stripeSubId.isBlank()) {
            // No Stripe record yet — reflect local state
            return new ProviderSubscriptionState(
                    currentSubscription.getTier(),
                    currentSubscription.getStatus(),
                    currentSubscription.getProviderCustomerId(),
                    null,
                    currentSubscription.getCurrentPeriodStart(),
                    currentSubscription.getCurrentPeriodEnd()
            );
        }

        try {
            Subscription stripeSub = Subscription.retrieve(
                    stripeSubId,
                    SubscriptionRetrieveParams.builder().build(),
                    null);
            return toState(stripeSub, currentSubscription.getProviderCustomerId());
        } catch (StripeException e) {
            log.error("Stripe getStatus failed for user {}: {}", user.getId(), e.getMessage());
            throw DomainExceptions.billingProviderError(e.getMessage());
        }
    }

    @Override
    public BillingProvider getProvider() {
        return BillingProvider.STRIPE;
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void requireCredentials() {
        if (!stripeProperties.isConfigured()) {
            throw DomainExceptions.billingProviderError(
                    "Stripe credentials are not configured. Set STRIPE_SECRET_KEY and STRIPE_PRICE_PRO_MONTHLY.");
        }
    }

    private void configureStripe() {
        Stripe.apiKey = stripeProperties.getSecretKey();
    }

    /**
     * Finds an existing Stripe customer by email or creates a new one.
     * Uses the providerCustomerId stored on the local Subscription if available.
     */
    private String resolveOrCreateCustomer(User user,
            com.careerforge.backend.billing.Subscription currentSubscription) throws StripeException {

        // Use stored customer ID if present
        if (currentSubscription.getProviderCustomerId() != null
                && !currentSubscription.getProviderCustomerId().isBlank()) {
            return currentSubscription.getProviderCustomerId();
        }

        // Search Stripe for existing customer by email
        CustomerSearchResult result = Customer.search(
                CustomerSearchParams.builder()
                        .setQuery("email:'" + user.getEmail() + "'")
                        .setLimit(1L)
                        .build());

        if (!result.getData().isEmpty()) {
            return result.getData().get(0).getId();
        }

        // Create new customer
        Customer customer = Customer.create(
                CustomerCreateParams.builder()
                        .setEmail(user.getEmail())
                        .putMetadata("careerforge_user_id", user.getId().toString())
                        .build());

        return customer.getId();
    }

    private Subscription createSubscription(String customerId) throws StripeException {
        return Subscription.create(
                SubscriptionCreateParams.builder()
                        .setCustomer(customerId)
                        .addItem(SubscriptionCreateParams.Item.builder()
                                .setPrice(stripeProperties.getPriceProMonthly())
                                .build())
                        .build());
    }

    /**
     * Maps a Stripe Subscription to the provider-agnostic ProviderSubscriptionState.
     * No Stripe types escape this method.
     */
    private ProviderSubscriptionState toState(Subscription stripeSub, String customerId) {
        SubscriptionTier tier = "active".equals(stripeSub.getStatus())
                || "trialing".equals(stripeSub.getStatus())
                ? SubscriptionTier.PRO
                : SubscriptionTier.FREE;

        SubscriptionStatus status = mapStripeStatus(stripeSub.getStatus());

        Instant periodStart = stripeSub.getCurrentPeriodStart() != null
                ? Instant.ofEpochSecond(stripeSub.getCurrentPeriodStart())
                : null;
        Instant periodEnd = stripeSub.getCurrentPeriodEnd() != null
                ? Instant.ofEpochSecond(stripeSub.getCurrentPeriodEnd())
                : null;

        return new ProviderSubscriptionState(
                tier,
                status,
                customerId,
                stripeSub.getId(),
                periodStart,
                periodEnd
        );
    }

    private SubscriptionStatus mapStripeStatus(String stripeStatus) {
        if (stripeStatus == null) return SubscriptionStatus.INACTIVE;
        return switch (stripeStatus) {
            case "active", "trialing" -> SubscriptionStatus.ACTIVE;
            case "canceled" -> SubscriptionStatus.CANCELED;
            case "past_due", "unpaid" -> SubscriptionStatus.PAST_DUE;
            default -> SubscriptionStatus.INACTIVE;
        };
    }
}
