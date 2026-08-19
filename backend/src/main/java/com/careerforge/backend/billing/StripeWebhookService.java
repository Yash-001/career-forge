package com.careerforge.backend.billing;

import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.auth.repository.UserRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeWebhookService {

    private final StripeProperties stripeProperties;
    private final StripeWebhookEventRepository webhookEventRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    /**
     * Verifies the Stripe webhook signature and returns the parsed Event.
     * Throws SignatureVerificationException on invalid signature.
     */
    public Event verifyAndParse(String payload, String sigHeader) throws SignatureVerificationException {
        String secret = stripeProperties.getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            throw new SignatureVerificationException(
                    "Webhook secret is not configured.", sigHeader);
        }
        return Webhook.constructEvent(payload, sigHeader, secret);
    }

    /**
     * Processes a verified Stripe event idempotently.
     * Returns true if the event was processed, false if it was a duplicate.
     */
    @Transactional
    public boolean process(Event event) {
        if (webhookEventRepository.existsByProviderEventId(event.getId())) {
            log.debug("Duplicate Stripe event ignored: {}", event.getId());
            return false;
        }

        handleEvent(event);

        webhookEventRepository.save(StripeWebhookEvent.builder()
                .providerEventId(event.getId())
                .eventType(event.getType())
                .build());

        return true;
    }

    // ── Event dispatch ────────────────────────────────────────────────────────

    private void handleEvent(Event event) {
        switch (event.getType()) {
            case "customer.subscription.created",
                 "customer.subscription.updated" -> handleSubscriptionUpsert(event);
            case "customer.subscription.deleted"  -> handleSubscriptionDeleted(event);
            case "invoice.payment_failed"          -> handlePaymentFailed(event);
            default -> log.debug("Unhandled Stripe event type: {}", event.getType());
        }
    }

    private void handleSubscriptionUpsert(Event event) {
        deserializeSubscription(event).ifPresent(stripeSub -> {
            findLocalSubscription(stripeSub).ifPresent(local -> {
                SubscriptionStatus status = mapStatus(stripeSub.getStatus());
                SubscriptionTier tier = isTierActive(status) ? SubscriptionTier.PRO : SubscriptionTier.FREE;

                local.setTier(tier);
                local.setStatus(status);
                local.setProviderCustomerId(stripeSub.getCustomer());
                local.setProviderSubscriptionId(stripeSub.getId());
                if (stripeSub.getCurrentPeriodStart() != null) {
                    local.setCurrentPeriodStart(Instant.ofEpochSecond(stripeSub.getCurrentPeriodStart()));
                }
                if (stripeSub.getCurrentPeriodEnd() != null) {
                    local.setCurrentPeriodEnd(Instant.ofEpochSecond(stripeSub.getCurrentPeriodEnd()));
                }
                subscriptionRepository.save(local);
                syncUserTier(local, tier);
                log.info("Subscription {} updated to {}/{} via webhook", local.getId(), tier, status);
            });
        });
    }

    private void handleSubscriptionDeleted(Event event) {
        deserializeSubscription(event).ifPresent(stripeSub -> {
            findLocalSubscription(stripeSub).ifPresent(local -> {
                local.setTier(SubscriptionTier.FREE);
                local.setStatus(SubscriptionStatus.CANCELED);
                subscriptionRepository.save(local);
                syncUserTier(local, SubscriptionTier.FREE);
                log.info("Subscription {} canceled via webhook", local.getId());
            });
        });
    }

    private void handlePaymentFailed(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (!deserializer.getObject().isPresent()) {
            log.warn("Could not deserialize invoice for event {}", event.getId());
            return;
        }
        StripeObject obj = deserializer.getObject().get();
        if (!(obj instanceof com.stripe.model.Invoice invoice)) {
            return;
        }
        String stripeSubId = invoice.getSubscription();
        if (stripeSubId == null || stripeSubId.isBlank()) return;

        subscriptionRepository.findByProviderSubscriptionId(stripeSubId).ifPresent(local -> {
            local.setStatus(SubscriptionStatus.PAST_DUE);
            subscriptionRepository.save(local);
            log.info("Subscription {} marked PAST_DUE via invoice.payment_failed webhook", local.getId());
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Optional<com.stripe.model.Subscription> deserializeSubscription(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (!deserializer.getObject().isPresent()) {
            log.warn("Could not deserialize subscription for event {}", event.getId());
            return Optional.empty();
        }
        StripeObject obj = deserializer.getObject().get();
        if (!(obj instanceof com.stripe.model.Subscription sub)) {
            return Optional.empty();
        }
        return Optional.of(sub);
    }

    /**
     * Finds the local Subscription by provider subscription ID first,
     * then falls back to provider customer ID.
     */
    private Optional<Subscription> findLocalSubscription(com.stripe.model.Subscription stripeSub) {
        if (stripeSub.getId() != null) {
            Optional<Subscription> bySubId = subscriptionRepository
                    .findByProviderSubscriptionId(stripeSub.getId());
            if (bySubId.isPresent()) return bySubId;
        }
        if (stripeSub.getCustomer() != null) {
            return subscriptionRepository.findByProviderCustomerId(stripeSub.getCustomer());
        }
        log.warn("No local subscription found for Stripe sub {} / customer {}",
                stripeSub.getId(), stripeSub.getCustomer());
        return Optional.empty();
    }

    private void syncUserTier(Subscription sub, SubscriptionTier tier) {
        userRepository.findById(sub.getUser().getId()).ifPresent(user -> {
            user.setSubscriptionTier(tier);
            userRepository.save(user);
        });
    }

    private SubscriptionStatus mapStatus(String stripeStatus) {
        if (stripeStatus == null) return SubscriptionStatus.INACTIVE;
        return switch (stripeStatus) {
            case "active", "trialing" -> SubscriptionStatus.ACTIVE;
            case "canceled"           -> SubscriptionStatus.CANCELED;
            case "past_due", "unpaid" -> SubscriptionStatus.PAST_DUE;
            default                   -> SubscriptionStatus.INACTIVE;
        };
    }

    private boolean isTierActive(SubscriptionStatus status) {
        return status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.PAST_DUE;
    }
}
