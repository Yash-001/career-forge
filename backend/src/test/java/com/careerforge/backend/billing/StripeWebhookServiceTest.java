package com.careerforge.backend.billing;

import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.auth.repository.UserRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeWebhookServiceTest {

    @Mock StripeProperties stripeProperties;
    @Mock StripeWebhookEventRepository webhookEventRepository;
    @Mock SubscriptionRepository subscriptionRepository;
    @Mock UserRepository userRepository;

    @InjectMocks StripeWebhookService webhookService;

    private User user;
    private Subscription localSub;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("hash")
                .subscriptionTier(SubscriptionTier.FREE)
                .enabled(true)
                .build();

        localSub = Subscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tier(SubscriptionTier.FREE)
                .status(SubscriptionStatus.ACTIVE)
                .provider(BillingProvider.STRIPE)
                .providerCustomerId("cus_abc")
                .providerSubscriptionId("sub_abc")
                .build();
    }

    // ── 1. Valid webhook signature ────────────────────────────────────────────

    @Test
    void verifyAndParse_validSignature_returnsEvent() throws Exception {
        when(stripeProperties.getWebhookSecret()).thenReturn("whsec_test");

        Event mockEvent = mock(Event.class);

        try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
            webhookStatic.when(() -> Webhook.constructEvent("payload", "sig", "whsec_test"))
                    .thenReturn(mockEvent);

            Event result = webhookService.verifyAndParse("payload", "sig");

            assertThat(result).isSameAs(mockEvent);
        }
    }

    // ── 2. Invalid signature ──────────────────────────────────────────────────

    @Test
    void verifyAndParse_invalidSignature_throwsSignatureVerificationException() throws Exception {
        when(stripeProperties.getWebhookSecret()).thenReturn("whsec_test");

        try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
            webhookStatic.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenThrow(new SignatureVerificationException("Bad sig", "sig"));

            assertThatThrownBy(() -> webhookService.verifyAndParse("payload", "bad_sig"))
                    .isInstanceOf(SignatureVerificationException.class);
        }
    }

    // ── 3. Duplicate webhook (idempotency) ────────────────────────────────────

    @Test
    void process_duplicateEvent_returnsFalseAndSkipsProcessing() {
        Event event = mock(Event.class);
        when(event.getId()).thenReturn("evt_dup");
        when(webhookEventRepository.existsByProviderEventId("evt_dup")).thenReturn(true);

        boolean processed = webhookService.process(event);

        assertThat(processed).isFalse();
        verify(subscriptionRepository, never()).save(any());
        verify(webhookEventRepository, never()).save(any());
    }

    // ── 4. Subscription activation ────────────────────────────────────────────

    @Test
    void process_subscriptionUpdated_active_upgradesLocalSubscription() {
        com.stripe.model.Subscription stripeSub = buildStripeSubscription("sub_abc", "active", "cus_abc");
        Event event = buildEvent("evt_001", "customer.subscription.updated", stripeSub);

        when(webhookEventRepository.existsByProviderEventId("evt_001")).thenReturn(false);
        when(subscriptionRepository.findByProviderSubscriptionId("sub_abc")).thenReturn(Optional.of(localSub));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        boolean processed = webhookService.process(event);

        assertThat(processed).isTrue();
        assertThat(localSub.getTier()).isEqualTo(SubscriptionTier.PRO);
        assertThat(localSub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(subscriptionRepository).save(localSub);
        verify(webhookEventRepository).save(any(StripeWebhookEvent.class));
    }

    // ── 5. Subscription cancellation ─────────────────────────────────────────

    @Test
    void process_subscriptionDeleted_cancelsLocalSubscription() {
        // deleted handler only reads getId() — findLocalSubscription short-circuits on sub ID match
        com.stripe.model.Subscription stripeSub = mock(com.stripe.model.Subscription.class);
        when(stripeSub.getId()).thenReturn("sub_abc");
        Event event = buildEvent("evt_002", "customer.subscription.deleted", stripeSub);

        when(webhookEventRepository.existsByProviderEventId("evt_002")).thenReturn(false);
        when(subscriptionRepository.findByProviderSubscriptionId("sub_abc")).thenReturn(Optional.of(localSub));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        boolean processed = webhookService.process(event);

        assertThat(processed).isTrue();
        assertThat(localSub.getTier()).isEqualTo(SubscriptionTier.FREE);
        assertThat(localSub.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        verify(subscriptionRepository).save(localSub);
    }

    // ── 6. Payment failure ────────────────────────────────────────────────────

    @Test
    void process_invoicePaymentFailed_marksSubscriptionPastDue() {
        Invoice invoice = mock(Invoice.class);
        when(invoice.getSubscription()).thenReturn("sub_abc");

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(invoice));

        Event event = mock(Event.class);
        when(event.getId()).thenReturn("evt_003");
        when(event.getType()).thenReturn("invoice.payment_failed");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);

        when(webhookEventRepository.existsByProviderEventId("evt_003")).thenReturn(false);
        when(subscriptionRepository.findByProviderSubscriptionId("sub_abc")).thenReturn(Optional.of(localSub));

        boolean processed = webhookService.process(event);

        assertThat(processed).isTrue();
        assertThat(localSub.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
        verify(subscriptionRepository).save(localSub);
    }

    // ── 7. Unknown event type ─────────────────────────────────────────────────

    @Test
    void process_unknownEventType_recordsEventAndDoesNothing() {
        Event event = mock(Event.class);
        when(event.getId()).thenReturn("evt_004");
        when(event.getType()).thenReturn("payment_intent.created");

        when(webhookEventRepository.existsByProviderEventId("evt_004")).thenReturn(false);

        boolean processed = webhookService.process(event);

        assertThat(processed).isTrue();
        verify(subscriptionRepository, never()).save(any());
        verify(webhookEventRepository).save(any(StripeWebhookEvent.class));
    }

    // ── 8. Malformed event (no matching local subscription) ───────────────────

    @Test
    void process_subscriptionUpdated_noMatchingLocalSub_recordsEventGracefully() {
        // period fields are never reached when no local sub is found — use minimal stub
        com.stripe.model.Subscription stripeSub = mock(com.stripe.model.Subscription.class);
        when(stripeSub.getId()).thenReturn("sub_unknown");
        when(stripeSub.getCustomer()).thenReturn("cus_unknown");
        Event event = buildEvent("evt_005", "customer.subscription.updated", stripeSub);

        when(webhookEventRepository.existsByProviderEventId("evt_005")).thenReturn(false);
        when(subscriptionRepository.findByProviderSubscriptionId("sub_unknown")).thenReturn(Optional.empty());
        when(subscriptionRepository.findByProviderCustomerId("cus_unknown")).thenReturn(Optional.empty());

        boolean processed = webhookService.process(event);

        // Event is still recorded to prevent reprocessing
        assertThat(processed).isTrue();
        verify(subscriptionRepository, never()).save(any());
        verify(webhookEventRepository).save(any(StripeWebhookEvent.class));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private com.stripe.model.Subscription buildStripeSubscription(String id, String status, String customerId) {
        com.stripe.model.Subscription sub = mock(com.stripe.model.Subscription.class);
        when(sub.getId()).thenReturn(id);
        when(sub.getStatus()).thenReturn(status);
        when(sub.getCustomer()).thenReturn(customerId);
        when(sub.getCurrentPeriodStart()).thenReturn(1700000000L);
        when(sub.getCurrentPeriodEnd()).thenReturn(1702592000L);
        return sub;
    }

    private Event buildEvent(String eventId, String type, com.stripe.model.Subscription stripeSub) {
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(stripeSub));

        Event event = mock(Event.class);
        when(event.getId()).thenReturn(eventId);
        when(event.getType()).thenReturn(type);
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        return event;
    }
}
