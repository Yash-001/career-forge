package com.careerforge.backend.billing;

import com.careerforge.backend.AbstractIntegrationTest;
import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.auth.repository.UserRepository;
import com.careerforge.backend.pdf.domain.PdfExportUsage;
import com.careerforge.backend.pdf.repository.PdfExportUsageRepository;
import com.careerforge.backend.pdf.service.ExportLimitService;
import com.careerforge.backend.profile.repository.MasterProfileRepository;
import com.careerforge.backend.shared.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class BillingServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired SubscriptionService subscriptionService;
    @Autowired BillingService billingService;
    @Autowired BillingProviderPort billingProviderPort;
    @Autowired MasterProfileRepository masterProfileRepository;
    @Autowired PdfExportUsageRepository usageRepository;
    @Autowired ExportLimitService exportLimitService;

    @BeforeEach
    void clean() {
        usageRepository.deleteAll();
        subscriptionRepository.deleteAll();
        masterProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ── 1. BillingService uses provider abstraction ───────────────────────────

    @Test
    void billingService_usesProviderAbstraction() {
        assertThat(billingProviderPort).isInstanceOf(DemoBillingProvider.class);
        assertThat(billingProviderPort.getProvider()).isEqualTo(BillingProvider.DEMO);
    }

    // ── 2. Demo upgrade FREE → PRO ────────────────────────────────────────────

    @Test
    void demoUpgrade_freeToProSucceeds() {
        User user = createUserWithSubscription("upgrade@example.com", SubscriptionTier.FREE);

        Subscription result = billingService.upgrade(user);

        assertThat(result.getTier()).isEqualTo(SubscriptionTier.PRO);
        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(result.getCurrentPeriodStart()).isNotNull();
        assertThat(result.getCurrentPeriodEnd()).isNotNull();

        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getSubscriptionTier()).isEqualTo(SubscriptionTier.PRO);
    }

    // ── 3. Demo cancellation PRO → CANCELED ──────────────────────────────────

    @Test
    void demoCancel_proToCanceledSucceeds() {
        User user = createUserWithSubscription("cancel@example.com", SubscriptionTier.FREE);
        billingService.upgrade(user);

        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        Subscription canceled = billingService.cancel(reloaded);

        assertThat(canceled.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        assertThat(canceled.getTier()).isEqualTo(SubscriptionTier.FREE);

        User afterCancel = userRepository.findById(user.getId()).orElseThrow();
        assertThat(afterCancel.getSubscriptionTier()).isEqualTo(SubscriptionTier.FREE);
    }

    // ── 4. Demo subscription status retrieval ────────────────────────────────

    @Test
    void demoGetStatus_returnsCurrentState() {
        User user = createUserWithSubscription("status@example.com", SubscriptionTier.FREE);

        ProviderSubscriptionState state = billingService.getStatus(user);

        assertThat(state.tier()).isEqualTo(SubscriptionTier.FREE);
        assertThat(state.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(state.providerCustomerId()).isNull();
        assertThat(state.providerSubscriptionId()).isNull();
    }

    // ── 5. Provider errors are translated into domain errors ──────────────────

    @Test
    void upgrade_withNoActiveSubscription_throwsDomainError() {
        User user = userRepository.save(User.builder()
                .email("nosub@example.com")
                .passwordHash(passwordEncoder.encode("Password1"))
                .subscriptionTier(SubscriptionTier.FREE)
                .enabled(true)
                .build());

        assertThatThrownBy(() -> billingService.upgrade(user))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("NO_ACTIVE_SUBSCRIPTION");
    }

    @Test
    void upgrade_alreadyPro_throwsDomainError() {
        User user = createUserWithSubscription("alreadypro@example.com", SubscriptionTier.FREE);
        billingService.upgrade(user);
        User reloaded = userRepository.findById(user.getId()).orElseThrow();

        assertThatThrownBy(() -> billingService.upgrade(reloaded))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("ALREADY_PRO");
    }

    @Test
    void cancel_withNoActiveSubscription_throwsDomainError() {
        User user = userRepository.save(User.builder()
                .email("cancelnosub@example.com")
                .passwordHash(passwordEncoder.encode("Password1"))
                .subscriptionTier(SubscriptionTier.FREE)
                .enabled(true)
                .build());

        assertThatThrownBy(() -> billingService.cancel(user))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("NO_ACTIVE_SUBSCRIPTION");
    }

    // ── 6. BillingService does not depend directly on DemoBillingProvider ─────

    @Test
    void billingService_isNotInstanceOfDemoBillingProvider() {
        assertThat(billingService).isNotInstanceOf(DemoBillingProvider.class);
        assertThat(billingService).isInstanceOf(DefaultBillingService.class);
    }

    // ── 7. Stripe SDK is on classpath; demo provider is active in test profile ─

    @Test
    void stripeSdk_isOnClasspath() {
        // Stripe SDK is now a dependency — class must resolve
        assertThatCode(() -> Class.forName("com.stripe.Stripe"))
                .doesNotThrowAnyException();
    }

    @Test
    void activeProvider_isDemoInTestProfile() {
        // application-test.properties sets app.billing.provider=demo
        // so the injected BillingProviderPort must be DemoBillingProvider, not Stripe
        assertThat(billingProviderPort).isInstanceOf(DemoBillingProvider.class);
        assertThat(billingProviderPort).isNotInstanceOf(StripeBillingProvider.class);
    }

    // ── 8. Existing PDF limits continue working after upgrade ─────────────────

    @Test
    void pdfExportLimit_bypassedAfterUpgrade() {
        User user = createUserWithSubscription("pdflimit@example.com", SubscriptionTier.FREE);

        usageRepository.save(PdfExportUsage.builder()
                .user(user)
                .billingPeriod(LocalDate.now().withDayOfMonth(1))
                .exportCount(3)
                .build());

        assertThatThrownBy(() -> exportLimitService.checkLimit(user))
                .hasMessageContaining("PDF export limit");

        billingService.upgrade(user);
        User reloaded = userRepository.findById(user.getId()).orElseThrow();

        exportLimitService.checkLimit(reloaded); // must not throw
    }

    @Test
    void pdfExportLimit_reEnforcedAfterCancel() {
        User user = createUserWithSubscription("pdfcancel@example.com", SubscriptionTier.FREE);
        billingService.upgrade(user);
        User upgraded = userRepository.findById(user.getId()).orElseThrow();

        usageRepository.save(PdfExportUsage.builder()
                .user(upgraded)
                .billingPeriod(LocalDate.now().withDayOfMonth(1))
                .exportCount(3)
                .build());

        exportLimitService.checkLimit(upgraded);

        billingService.cancel(upgraded);
        User canceled = userRepository.findById(user.getId()).orElseThrow();

        assertThatThrownBy(() -> exportLimitService.checkLimit(canceled))
                .hasMessageContaining("PDF export limit");
    }

    // ── 9. Upgrade persists to DB correctly ───────────────────────────────────

    @Test
    void upgrade_persistsToDatabase() {
        User user = createUserWithSubscription("persist@example.com", SubscriptionTier.FREE);
        billingService.upgrade(user);

        List<Subscription> subs = subscriptionRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId());
        assertThat(subs).hasSize(1);
        assertThat(subs.get(0).getTier()).isEqualTo(SubscriptionTier.PRO);
        assertThat(subs.get(0).getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    // ── 10. getStatus after upgrade reflects PRO ──────────────────────────────

    @Test
    void getStatus_afterUpgrade_returnsPro() {
        User user = createUserWithSubscription("statusupgrade@example.com", SubscriptionTier.FREE);
        billingService.upgrade(user);
        User reloaded = userRepository.findById(user.getId()).orElseThrow();

        ProviderSubscriptionState state = billingService.getStatus(reloaded);

        assertThat(state.tier()).isEqualTo(SubscriptionTier.PRO);
        assertThat(state.status()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User createUserWithSubscription(String email, SubscriptionTier tier) {
        User user = userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Password1"))
                .subscriptionTier(tier)
                .enabled(true)
                .build());
        subscriptionService.provisionFreeSubscription(user);
        return user;
    }
}
