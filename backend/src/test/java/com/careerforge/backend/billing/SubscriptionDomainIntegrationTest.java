package com.careerforge.backend.billing;

import com.careerforge.backend.AbstractIntegrationTest;
import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.auth.repository.UserRepository;
import com.careerforge.backend.pdf.domain.PdfExportUsage;
import com.careerforge.backend.pdf.repository.PdfExportUsageRepository;
import com.careerforge.backend.pdf.service.ExportLimitService;
import com.careerforge.backend.profile.repository.MasterProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class SubscriptionDomainIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired SubscriptionService subscriptionService;
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

    // ── 1. New user registration provisions a FREE subscription ──────────────

    @Test
    void registration_provisionsFreeSubscription() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "billing1@example.com",
                                "password", "Password1"
                        ))))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail("billing1@example.com").orElseThrow();
        List<Subscription> subs = subscriptionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        assertThat(subs).hasSize(1);
        Subscription sub = subs.get(0);
        assertThat(sub.getTier()).isEqualTo(SubscriptionTier.FREE);
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(sub.getProvider()).isEqualTo(BillingProvider.DEMO);
    }

    // ── 2. FREE subscription fields are represented correctly ─────────────────

    @Test
    void freeSubscription_fieldsAreCorrect() {
        User user = createUser("billing2@example.com", SubscriptionTier.FREE);
        Subscription sub = subscriptionService.provisionFreeSubscription(user);

        assertThat(sub.getId()).isNotNull();
        assertThat(sub.getUser().getId()).isEqualTo(user.getId());
        assertThat(sub.getTier()).isEqualTo(SubscriptionTier.FREE);
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(sub.getProvider()).isEqualTo(BillingProvider.DEMO);
        assertThat(sub.getProviderCustomerId()).isNull();
        assertThat(sub.getProviderSubscriptionId()).isNull();
        assertThat(sub.getCreatedAt()).isNotNull();
        assertThat(sub.getUpdatedAt()).isNotNull();
    }

    // ── 3. PRO subscription can be persisted and retrieved ────────────────────

    @Test
    void proSubscription_canBePersistedAndRetrieved() {
        User user = createUser("billing3@example.com", SubscriptionTier.PRO);
        Subscription sub = subscriptionRepository.save(Subscription.builder()
                .user(user)
                .tier(SubscriptionTier.PRO)
                .status(SubscriptionStatus.ACTIVE)
                .provider(BillingProvider.STRIPE)
                .providerCustomerId("cus_test_123")
                .providerSubscriptionId("sub_test_456")
                .currentPeriodStart(Instant.now())
                .currentPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS))
                .build());

        Subscription loaded = subscriptionRepository.findById(sub.getId()).orElseThrow();
        assertThat(loaded.getTier()).isEqualTo(SubscriptionTier.PRO);
        assertThat(loaded.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(loaded.getProvider()).isEqualTo(BillingProvider.STRIPE);
    }

    // ── 4. Subscription status persists correctly ─────────────────────────────

    @Test
    void subscriptionStatus_persistsAllValues() {
        User user = createUser("billing4@example.com", SubscriptionTier.FREE);

        for (SubscriptionStatus status : SubscriptionStatus.values()) {
            Subscription sub = subscriptionRepository.save(Subscription.builder()
                    .user(user)
                    .tier(SubscriptionTier.FREE)
                    .status(status)
                    .provider(BillingProvider.DEMO)
                    .build());

            Subscription loaded = subscriptionRepository.findById(sub.getId()).orElseThrow();
            assertThat(loaded.getStatus()).isEqualTo(status);

            // Clean up so the partial unique index on ACTIVE doesn't block next iteration
            subscriptionRepository.delete(sub);
        }
    }

    // ── 5. Provider persists correctly ────────────────────────────────────────

    @Test
    void provider_persistsBothValues() {
        User user = createUser("billing5@example.com", SubscriptionTier.FREE);

        Subscription demo = subscriptionRepository.save(Subscription.builder()
                .user(user).tier(SubscriptionTier.FREE)
                .status(SubscriptionStatus.INACTIVE).provider(BillingProvider.DEMO).build());
        Subscription stripe = subscriptionRepository.save(Subscription.builder()
                .user(user).tier(SubscriptionTier.PRO)
                .status(SubscriptionStatus.CANCELED).provider(BillingProvider.STRIPE).build());

        assertThat(subscriptionRepository.findById(demo.getId()).orElseThrow().getProvider())
                .isEqualTo(BillingProvider.DEMO);
        assertThat(subscriptionRepository.findById(stripe.getId()).orElseThrow().getProvider())
                .isEqualTo(BillingProvider.STRIPE);
    }

    // ── 6. Provider customer/subscription IDs persist ─────────────────────────

    @Test
    void providerIds_persistCorrectly() {
        User user = createUser("billing6@example.com", SubscriptionTier.PRO);
        Subscription sub = subscriptionRepository.save(Subscription.builder()
                .user(user)
                .tier(SubscriptionTier.PRO)
                .status(SubscriptionStatus.ACTIVE)
                .provider(BillingProvider.STRIPE)
                .providerCustomerId("cus_abc123")
                .providerSubscriptionId("sub_xyz789")
                .build());

        Subscription loaded = subscriptionRepository.findById(sub.getId()).orElseThrow();
        assertThat(loaded.getProviderCustomerId()).isEqualTo("cus_abc123");
        assertThat(loaded.getProviderSubscriptionId()).isEqualTo("sub_xyz789");
    }

    // ── 7. Billing period fields persist ──────────────────────────────────────

    @Test
    void billingPeriod_persistsCorrectly() {
        User user = createUser("billing7@example.com", SubscriptionTier.PRO);
        Instant start = Instant.parse("2025-01-01T00:00:00Z");
        Instant end = Instant.parse("2025-02-01T00:00:00Z");

        Subscription sub = subscriptionRepository.save(Subscription.builder()
                .user(user)
                .tier(SubscriptionTier.PRO)
                .status(SubscriptionStatus.ACTIVE)
                .provider(BillingProvider.STRIPE)
                .currentPeriodStart(start)
                .currentPeriodEnd(end)
                .build());

        Subscription loaded = subscriptionRepository.findById(sub.getId()).orElseThrow();
        assertThat(loaded.getCurrentPeriodStart()).isEqualTo(start);
        assertThat(loaded.getCurrentPeriodEnd()).isEqualTo(end);
    }

    // ── 8. One user cannot have two ACTIVE subscriptions ─────────────────────

    @Test
    void oneActiveSubscriptionPerUser_partialUniqueIndexEnforced() {
        User user = createUser("billing8@example.com", SubscriptionTier.FREE);

        subscriptionRepository.save(Subscription.builder()
                .user(user).tier(SubscriptionTier.FREE)
                .status(SubscriptionStatus.ACTIVE).provider(BillingProvider.DEMO).build());

        // Attempting a second ACTIVE subscription must fail at the DB level
        assertThatThrownBy(() -> {
            subscriptionRepository.saveAndFlush(Subscription.builder()
                    .user(user).tier(SubscriptionTier.PRO)
                    .status(SubscriptionStatus.ACTIVE).provider(BillingProvider.STRIPE).build());
        }).isInstanceOf(Exception.class);
    }

    // ── 9. provisionFreeSubscription is idempotent ────────────────────────────

    @Test
    void provisionFreeSubscription_isIdempotent() {
        User user = createUser("billing9@example.com", SubscriptionTier.FREE);

        Subscription first = subscriptionService.provisionFreeSubscription(user);
        Subscription second = subscriptionService.provisionFreeSubscription(user);

        assertThat(first.getId()).isEqualTo(second.getId());
        assertThat(subscriptionRepository.findByUserIdOrderByCreatedAtDesc(user.getId())).hasSize(1);
    }

    // ── 10. Existing PDF export limit logic still works ───────────────────────

    @Test
    void existingPdfExportLimit_stillEnforcedForFreeUser() {
        User user = createUser("billing10@example.com", SubscriptionTier.FREE);

        // Seed usage at the limit
        usageRepository.save(PdfExportUsage.builder()
                .user(user)
                .billingPeriod(LocalDate.now().withDayOfMonth(1))
                .exportCount(3)
                .build());

        assertThatThrownBy(() -> exportLimitService.checkLimit(user))
                .hasMessageContaining("PDF export limit");
    }

    // ── 11. Pro user bypasses PDF export limit ────────────────────────────────

    @Test
    void existingPdfExportLimit_notEnforcedForProUser() {
        User user = createUser("billing11@example.com", SubscriptionTier.PRO);

        // Even with usage at the limit, Pro user is not blocked
        usageRepository.save(PdfExportUsage.builder()
                .user(user)
                .billingPeriod(LocalDate.now().withDayOfMonth(1))
                .exportCount(100)
                .build());

        // Should not throw
        exportLimitService.checkLimit(user);
    }

    // ── 12. findActiveSubscription returns correct result ─────────────────────

    @Test
    void findActiveSubscription_returnsActiveOnly() {
        User user = createUser("billing12@example.com", SubscriptionTier.FREE);

        // No subscription yet
        Optional<Subscription> empty = subscriptionService.findActiveSubscription(user);
        assertThat(empty).isEmpty();

        // Provision one
        subscriptionService.provisionFreeSubscription(user);
        Optional<Subscription> active = subscriptionService.findActiveSubscription(user);
        assertThat(active).isPresent();
        assertThat(active.get().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User createUser(String email, SubscriptionTier tier) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Password1"))
                .subscriptionTier(tier)
                .enabled(true)
                .build());
    }
}
