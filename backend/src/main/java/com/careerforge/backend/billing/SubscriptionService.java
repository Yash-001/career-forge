package com.careerforge.backend.billing;

import com.careerforge.backend.auth.domain.User;

import java.util.Optional;

public interface SubscriptionService {

    /**
     * Returns true if the user has an active Pro subscription and is not
     * subject to free-tier usage limits.
     * <p>
     * Existing callers (ExportLimitService) depend on this method — do not remove.
     */
    boolean isPro(User user);

    /**
     * Returns the user's current active Subscription record, if one exists.
     * Returns empty for users whose subscription has not yet been provisioned
     * or whose subscription is not in ACTIVE status.
     */
    Optional<Subscription> findActiveSubscription(User user);

    /**
     * Provisions a FREE/DEMO subscription for a newly registered user.
     * Idempotent — does nothing if an active subscription already exists.
     */
    Subscription provisionFreeSubscription(User user);
}
