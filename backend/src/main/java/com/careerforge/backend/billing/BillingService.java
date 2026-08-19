package com.careerforge.backend.billing;

import com.careerforge.backend.auth.domain.User;

/**
 * Domain service for billing operations.
 * Owns business logic: ownership, state validation, syncing User.subscriptionTier.
 * Delegates provider communication to BillingProviderPort.
 * Never depends on a concrete provider implementation.
 */
public interface BillingService {

    /**
     * Upgrades the user's subscription to PRO.
     * Validates that the user does not already have an active PRO subscription.
     * Syncs User.subscriptionTier after a successful upgrade.
     *
     * @return the updated Subscription record
     */
    Subscription upgrade(User user);

    /**
     * Cancels the user's active subscription.
     * Validates that the user has an active subscription to cancel.
     * Syncs User.subscriptionTier after cancellation.
     *
     * @return the updated Subscription record
     */
    Subscription cancel(User user);

    /**
     * Returns the current subscription state for the user by querying the provider.
     * Falls back to the local Subscription record if no active subscription exists.
     *
     * @return the current ProviderSubscriptionState
     */
    ProviderSubscriptionState getStatus(User user);
}
