package com.careerforge.backend.billing;

import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.auth.domain.User;
import org.springframework.stereotype.Service;

@Service
public class DefaultSubscriptionService implements SubscriptionService {

    @Override
    public boolean isPro(User user) {
        return SubscriptionTier.PRO.equals(user.getSubscriptionTier());
    }
}
