package com.careerforge.backend.billing;

import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.auth.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DefaultSubscriptionService implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    /**
     * Fast-path check using the denormalized User.subscriptionTier field.
     * Existing callers (ExportLimitService) continue to work unchanged.
     */
    @Override
    public boolean isPro(User user) {
        return SubscriptionTier.PRO.equals(user.getSubscriptionTier());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Subscription> findActiveSubscription(User user) {
        return subscriptionRepository.findActiveByUserId(user.getId());
    }

    /**
     * Creates a FREE/DEMO subscription for a new user.
     * Idempotent — returns the existing active subscription if one already exists.
     */
    @Override
    @Transactional
    public Subscription provisionFreeSubscription(User user) {
        return subscriptionRepository.findActiveByUserId(user.getId())
                .orElseGet(() -> subscriptionRepository.save(
                        Subscription.builder()
                                .user(user)
                                .tier(SubscriptionTier.FREE)
                                .status(SubscriptionStatus.ACTIVE)
                                .provider(BillingProvider.DEMO)
                                .build()
                ));
    }
}
