package com.careerforge.backend.billing;

import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.auth.repository.UserRepository;
import com.careerforge.backend.shared.exception.DomainExceptions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DefaultBillingService implements BillingService {

    private final BillingProviderPort billingProviderPort;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Subscription upgrade(User user) {
        Subscription sub = subscriptionRepository.findActiveByUserId(user.getId())
                .orElseThrow(DomainExceptions::noActiveSubscription);

        if (SubscriptionTier.PRO.equals(sub.getTier())) {
            throw DomainExceptions.alreadyPro();
        }

        ProviderSubscriptionState state = billingProviderPort.initiateUpgrade(user, sub);

        sub.setTier(state.tier());
        sub.setStatus(state.status());
        sub.setProviderCustomerId(state.providerCustomerId());
        sub.setProviderSubscriptionId(state.providerSubscriptionId());
        sub.setCurrentPeriodStart(state.currentPeriodStart());
        sub.setCurrentPeriodEnd(state.currentPeriodEnd());
        subscriptionRepository.save(sub);

        // Sync denormalized fast-read field on User
        user.setSubscriptionTier(state.tier());
        userRepository.save(user);

        return sub;
    }

    @Override
    @Transactional
    public Subscription cancel(User user) {
        Subscription sub = subscriptionRepository.findActiveByUserId(user.getId())
                .orElseThrow(DomainExceptions::noActiveSubscription);

        ProviderSubscriptionState state = billingProviderPort.cancelSubscription(user, sub);

        sub.setTier(state.tier());
        sub.setStatus(state.status());
        sub.setCurrentPeriodStart(state.currentPeriodStart());
        sub.setCurrentPeriodEnd(state.currentPeriodEnd());
        subscriptionRepository.save(sub);

        // Sync denormalized fast-read field on User
        user.setSubscriptionTier(state.tier());
        userRepository.save(user);

        return sub;
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderSubscriptionState getStatus(User user) {
        Subscription sub = subscriptionRepository.findActiveByUserId(user.getId())
                .orElseThrow(DomainExceptions::noActiveSubscription);

        return billingProviderPort.getSubscriptionState(user, sub);
    }
}
