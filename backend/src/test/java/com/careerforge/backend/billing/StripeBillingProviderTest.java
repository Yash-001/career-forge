package com.careerforge.backend.billing;

import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.shared.exception.ApiException;
import com.stripe.exception.AuthenticationException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerSearchResult;
import com.stripe.model.Subscription;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerSearchParams;
import com.stripe.param.SubscriptionCancelParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionRetrieveParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeBillingProviderTest {

    private StripeProperties stripeProperties;
    private StripeBillingProvider provider;

    private User user;
    private com.careerforge.backend.billing.Subscription localSub;

    @BeforeEach
    void setUp() {
        stripeProperties = new StripeProperties();
        stripeProperties.setSecretKey("sk_test_fake_key");
        stripeProperties.setPriceProMonthly("price_test_pro_monthly");
        stripeProperties.setWebhookSecret("whsec_test_secret");

        provider = new StripeBillingProvider(stripeProperties);

        user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("hash")
                .subscriptionTier(SubscriptionTier.FREE)
                .enabled(true)
                .build();

        localSub = com.careerforge.backend.billing.Subscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tier(SubscriptionTier.FREE)
                .status(SubscriptionStatus.ACTIVE)
                .provider(BillingProvider.STRIPE)
                .build();
    }

    // ── getProvider ───────────────────────────────────────────────────────────

    @Test
    void getProvider_returnsStripe() {
        assertThat(provider.getProvider()).isEqualTo(BillingProvider.STRIPE);
    }

    // ── Missing credentials ───────────────────────────────────────────────────

    @Test
    void initiateUpgrade_missingSecretKey_throwsBillingProviderError() {
        stripeProperties.setSecretKey(null);

        assertThatThrownBy(() -> provider.initiateUpgrade(user, localSub))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("BILLING_PROVIDER_ERROR");
    }

    @Test
    void initiateUpgrade_blankSecretKey_throwsBillingProviderError() {
        stripeProperties.setSecretKey("   ");

        assertThatThrownBy(() -> provider.initiateUpgrade(user, localSub))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("BILLING_PROVIDER_ERROR");
    }

    @Test
    void initiateUpgrade_missingPriceId_throwsBillingProviderError() {
        stripeProperties.setPriceProMonthly(null);

        assertThatThrownBy(() -> provider.initiateUpgrade(user, localSub))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("BILLING_PROVIDER_ERROR");
    }

    @Test
    void cancelSubscription_missingCredentials_throwsBillingProviderError() {
        stripeProperties.setSecretKey(null);

        assertThatThrownBy(() -> provider.cancelSubscription(user, localSub))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("BILLING_PROVIDER_ERROR");
    }

    @Test
    void getSubscriptionState_missingCredentials_throwsBillingProviderError() {
        stripeProperties.setSecretKey(null);

        assertThatThrownBy(() -> provider.getSubscriptionState(user, localSub))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("BILLING_PROVIDER_ERROR");
    }

    // ── Cancel: missing provider subscription ID ──────────────────────────────

    @Test
    void cancelSubscription_noStripeSubId_throwsBillingProviderError() {
        // localSub has no providerSubscriptionId
        assertThatThrownBy(() -> provider.cancelSubscription(user, localSub))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("BILLING_PROVIDER_ERROR");
    }

    // ── Checkout / upgrade ────────────────────────────────────────────────────

    @Test
    void initiateUpgrade_createsNewCustomerAndSubscription_whenNoExistingCustomer() throws Exception {
        Customer mockCustomer = mock(Customer.class);
        when(mockCustomer.getId()).thenReturn("cus_new123");

        CustomerSearchResult emptyResult = mock(CustomerSearchResult.class);
        when(emptyResult.getData()).thenReturn(List.of());

        Subscription mockStripeSub = buildMockStripeSub("sub_abc", "active", 1700000000L, 1702592000L);

        try (MockedStatic<Customer> customerStatic = mockStatic(Customer.class);
             MockedStatic<Subscription> subStatic = mockStatic(Subscription.class)) {

            customerStatic.when(() -> Customer.search(any(CustomerSearchParams.class)))
                    .thenReturn(emptyResult);
            customerStatic.when(() -> Customer.create(any(CustomerCreateParams.class)))
                    .thenReturn(mockCustomer);
            subStatic.when(() -> Subscription.create(any(SubscriptionCreateParams.class)))
                    .thenReturn(mockStripeSub);

            ProviderSubscriptionState state = provider.initiateUpgrade(user, localSub);

            assertThat(state.tier()).isEqualTo(SubscriptionTier.PRO);
            assertThat(state.status()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(state.providerCustomerId()).isEqualTo("cus_new123");
            assertThat(state.providerSubscriptionId()).isEqualTo("sub_abc");
            assertThat(state.currentPeriodStart()).isNotNull();
            assertThat(state.currentPeriodEnd()).isNotNull();
        }
    }

    @Test
    void initiateUpgrade_reusesExistingCustomer_whenFoundByEmail() throws Exception {
        Customer existingCustomer = mock(Customer.class);
        when(existingCustomer.getId()).thenReturn("cus_existing456");

        CustomerSearchResult searchResult = mock(CustomerSearchResult.class);
        when(searchResult.getData()).thenReturn(List.of(existingCustomer));

        Subscription mockStripeSub = buildMockStripeSub("sub_xyz", "active", 1700000000L, 1702592000L);

        try (MockedStatic<Customer> customerStatic = mockStatic(Customer.class);
             MockedStatic<Subscription> subStatic = mockStatic(Subscription.class)) {

            customerStatic.when(() -> Customer.search(any(CustomerSearchParams.class)))
                    .thenReturn(searchResult);
            subStatic.when(() -> Subscription.create(any(SubscriptionCreateParams.class)))
                    .thenReturn(mockStripeSub);

            ProviderSubscriptionState state = provider.initiateUpgrade(user, localSub);

            assertThat(state.providerCustomerId()).isEqualTo("cus_existing456");
            // Customer.create must NOT have been called
            customerStatic.verify(() -> Customer.create(any(CustomerCreateParams.class)), never());
        }
    }

    @Test
    void initiateUpgrade_usesStoredCustomerId_whenPresentOnLocalSub() throws Exception {
        localSub.setProviderCustomerId("cus_stored789");

        Subscription mockStripeSub = buildMockStripeSub("sub_stored", "active", 1700000000L, 1702592000L);

        try (MockedStatic<Customer> customerStatic = mockStatic(Customer.class);
             MockedStatic<Subscription> subStatic = mockStatic(Subscription.class)) {

            subStatic.when(() -> Subscription.create(any(SubscriptionCreateParams.class)))
                    .thenReturn(mockStripeSub);

            ProviderSubscriptionState state = provider.initiateUpgrade(user, localSub);

            assertThat(state.providerCustomerId()).isEqualTo("cus_stored789");
            // Neither search nor create should be called
            customerStatic.verify(() -> Customer.search(any(CustomerSearchParams.class)), never());
            customerStatic.verify(() -> Customer.create(any(CustomerCreateParams.class)), never());
        }
    }

    // ── Cancellation ──────────────────────────────────────────────────────────

    @Test
    void cancelSubscription_cancelsStripeSubAndReturnsCanceledState() throws Exception {
        localSub.setProviderSubscriptionId("sub_to_cancel");
        localSub.setProviderCustomerId("cus_abc");

        Subscription activeSub = mock(Subscription.class);
        Subscription canceledSub = mock(Subscription.class);
        when(canceledSub.getId()).thenReturn("sub_to_cancel");
        when(canceledSub.getStatus()).thenReturn("canceled");
        when(activeSub.cancel(any(SubscriptionCancelParams.class))).thenReturn(canceledSub);

        try (MockedStatic<Subscription> subStatic = mockStatic(Subscription.class)) {
            subStatic.when(() -> Subscription.retrieve("sub_to_cancel"))
                    .thenReturn(activeSub);

            ProviderSubscriptionState state = provider.cancelSubscription(user, localSub);

            assertThat(state.status()).isEqualTo(SubscriptionStatus.CANCELED);
            assertThat(state.tier()).isEqualTo(SubscriptionTier.FREE);
        }
    }

    // ── Status retrieval ──────────────────────────────────────────────────────

    @Test
    void getSubscriptionState_noStripeSubId_returnsLocalState() {
        // No providerSubscriptionId — should reflect local record without calling Stripe
        localSub.setProviderCustomerId("cus_abc");

        ProviderSubscriptionState state = provider.getSubscriptionState(user, localSub);

        assertThat(state.tier()).isEqualTo(SubscriptionTier.FREE);
        assertThat(state.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(state.providerCustomerId()).isEqualTo("cus_abc");
        assertThat(state.providerSubscriptionId()).isNull();
    }

    @Test
    void getSubscriptionState_withStripeSubId_fetchesFromStripe() throws Exception {
        localSub.setProviderSubscriptionId("sub_live123");
        localSub.setProviderCustomerId("cus_abc");

        Subscription mockStripeSub = buildMockStripeSub("sub_live123", "active", 1700000000L, 1702592000L);

        try (MockedStatic<Subscription> subStatic = mockStatic(Subscription.class)) {
            subStatic.when(() -> Subscription.retrieve(
                    eq("sub_live123"),
                    any(SubscriptionRetrieveParams.class),
                    isNull()))
                    .thenReturn(mockStripeSub);

            ProviderSubscriptionState state = provider.getSubscriptionState(user, localSub);

            assertThat(state.tier()).isEqualTo(SubscriptionTier.PRO);
            assertThat(state.status()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(state.providerSubscriptionId()).isEqualTo("sub_live123");
        }
    }

    // ── Stripe status mapping ─────────────────────────────────────────────────

    @Test
    void stripeStatus_trialing_mapsToActiveAndPro() throws Exception {
        // No stored customer ID — forces customer search/create path
        CustomerSearchResult emptyResult = mock(CustomerSearchResult.class);
        when(emptyResult.getData()).thenReturn(List.of());

        Customer mockCustomer = mock(Customer.class);
        when(mockCustomer.getId()).thenReturn("cus_new");

        Subscription trialSub = mock(Subscription.class);
        when(trialSub.getId()).thenReturn("sub_trial");
        when(trialSub.getStatus()).thenReturn("trialing");

        try (MockedStatic<Customer> customerStatic = mockStatic(Customer.class);
             MockedStatic<Subscription> subStatic = mockStatic(Subscription.class)) {

            customerStatic.when(() -> Customer.search(any(CustomerSearchParams.class)))
                    .thenReturn(emptyResult);
            customerStatic.when(() -> Customer.create(any(CustomerCreateParams.class)))
                    .thenReturn(mockCustomer);
            subStatic.when(() -> Subscription.create(any(SubscriptionCreateParams.class)))
                    .thenReturn(trialSub);

            ProviderSubscriptionState state = provider.initiateUpgrade(user, localSub);

            assertThat(state.tier()).isEqualTo(SubscriptionTier.PRO);
            assertThat(state.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        }
    }

    @Test
    void stripeStatus_pastDue_mapsToPastDue() throws Exception {
        localSub.setProviderSubscriptionId("sub_pastdue");
        localSub.setProviderCustomerId("cus_abc");

        Subscription pastDueSub = buildMockStripeSub("sub_pastdue", "past_due", 1700000000L, 1702592000L);

        try (MockedStatic<Subscription> subStatic = mockStatic(Subscription.class)) {
            subStatic.when(() -> Subscription.retrieve(
                    eq("sub_pastdue"),
                    any(SubscriptionRetrieveParams.class),
                    isNull()))
                    .thenReturn(pastDueSub);

            ProviderSubscriptionState state = provider.getSubscriptionState(user, localSub);

            assertThat(state.status()).isEqualTo(SubscriptionStatus.PAST_DUE);
            assertThat(state.tier()).isEqualTo(SubscriptionTier.FREE);
        }
    }

    // ── Provider errors ───────────────────────────────────────────────────────

    @Test
    void initiateUpgrade_stripeException_throwsBillingProviderError() throws Exception {
        CustomerSearchResult emptyResult = mock(CustomerSearchResult.class);
        when(emptyResult.getData()).thenReturn(List.of());

        Customer mockCustomer = mock(Customer.class);
        when(mockCustomer.getId()).thenReturn("cus_new");

        try (MockedStatic<Customer> customerStatic = mockStatic(Customer.class);
             MockedStatic<Subscription> subStatic = mockStatic(Subscription.class)) {

            customerStatic.when(() -> Customer.search(any(CustomerSearchParams.class)))
                    .thenReturn(emptyResult);
            customerStatic.when(() -> Customer.create(any(CustomerCreateParams.class)))
                    .thenReturn(mockCustomer);
            subStatic.when(() -> Subscription.create(any(SubscriptionCreateParams.class)))
                    .thenThrow(new AuthenticationException("Invalid API key", "req_123", "401", 401));

            assertThatThrownBy(() -> provider.initiateUpgrade(user, localSub))
                    .isInstanceOf(ApiException.class)
                    .extracting("code").isEqualTo("BILLING_PROVIDER_ERROR");
        }
    }

    @Test
    void cancelSubscription_stripeException_throwsBillingProviderError() throws Exception {
        localSub.setProviderSubscriptionId("sub_err");

        try (MockedStatic<Subscription> subStatic = mockStatic(Subscription.class)) {
            subStatic.when(() -> Subscription.retrieve("sub_err"))
                    .thenThrow(new AuthenticationException("Invalid API key", "req_456", "401", 401));

            assertThatThrownBy(() -> provider.cancelSubscription(user, localSub))
                    .isInstanceOf(ApiException.class)
                    .extracting("code").isEqualTo("BILLING_PROVIDER_ERROR");
        }
    }

    @Test
    void getSubscriptionState_stripeException_throwsBillingProviderError() throws Exception {
        localSub.setProviderSubscriptionId("sub_err2");

        try (MockedStatic<Subscription> subStatic = mockStatic(Subscription.class)) {
            subStatic.when(() -> Subscription.retrieve(
                    eq("sub_err2"),
                    any(SubscriptionRetrieveParams.class),
                    isNull()))
                    .thenThrow(new AuthenticationException("Invalid API key", "req_789", "401", 401));

            assertThatThrownBy(() -> provider.getSubscriptionState(user, localSub))
                    .isInstanceOf(ApiException.class)
                    .extracting("code").isEqualTo("BILLING_PROVIDER_ERROR");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Subscription buildMockStripeSub(String id, String status,
            long periodStart, long periodEnd) {
        Subscription sub = mock(Subscription.class);
        when(sub.getId()).thenReturn(id);
        when(sub.getStatus()).thenReturn(status);
        when(sub.getCurrentPeriodStart()).thenReturn(periodStart);
        when(sub.getCurrentPeriodEnd()).thenReturn(periodEnd);
        return sub;
    }
}
