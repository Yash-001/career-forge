package com.careerforge.backend.billing;

import com.careerforge.backend.auth.domain.User;

/**
 * Provider abstraction for billing operations.
 * Implementations must not leak provider-specific SDK types into callers.
 * BillingService depends only on this interface — never on a concrete provider.
 */
public interface BillingProviderPort {

    /**
     * Initiates an upgrade to PRO for the given user.
     * For demo: immediately activates PRO.
     * For Stripe: creates a checkout session or direct subscription.
     *
     * @return the resulting subscription state from the provider
     */
    ProviderSubscriptionState initiateUpgrade(User user, Subscription currentSubscription);

    /**
     * Cancels the user's active subscription.
     * For demo: immediately marks as CANCELED.
     * For Stripe: cancels at period end or immediately depending on config.
     *
     * @return the resulting subscription state from the provider
     */
    ProviderSubscriptionState cancelSubscription(User user, Subscription currentSubscription);

    /**
     * Retrieves the current subscription state from the provider.
     * For demo: reads from the local Subscription record.
     * For Stripe: fetches from the Stripe API.
     *
     * @return the current subscription state, or null if no provider record exists
     */
    ProviderSubscriptionState getSubscriptionState(User user, Subscription currentSubscription);

    /**
     * Returns the BillingProvider enum value this port implementation represents.
     */
    BillingProvider getProvider();
}
