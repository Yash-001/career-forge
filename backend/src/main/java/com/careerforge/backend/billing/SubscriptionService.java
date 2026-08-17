package com.careerforge.backend.billing;

import com.careerforge.backend.auth.domain.User;

public interface SubscriptionService {

    /**
     * Returns true if the user has an active Pro subscription and is not
     * subject to free-tier usage limits.
     */
    boolean isPro(User user);
}
