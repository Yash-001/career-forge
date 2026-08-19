package com.careerforge.backend.billing;

import com.careerforge.backend.AbstractIntegrationTest;
import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.auth.repository.UserRepository;
import com.careerforge.backend.pdf.domain.PdfExportUsage;
import com.careerforge.backend.pdf.repository.PdfExportUsageRepository;
import com.careerforge.backend.shared.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class BillingApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired SubscriptionService subscriptionService;
    @Autowired BillingService billingService;
    @Autowired PdfExportUsageRepository usageRepository;

    @BeforeEach
    void clean() {
        usageRepository.deleteAll();
        subscriptionRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User createUser(String email) {
        User user = userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Password1"))
                .subscriptionTier(SubscriptionTier.FREE)
                .enabled(true)
                .build());
        subscriptionService.provisionFreeSubscription(user);
        return user;
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateAccessToken(user.getId(), user.getEmail());
    }

    // ── Unauthenticated → 401 ─────────────────────────────────────────────────

    @Test
    void getSubscription_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/billing/subscription"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void checkout_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/billing/checkout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cancel_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/billing/cancel"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /subscription ─────────────────────────────────────────────────────

    @Test
    void getSubscription_freeUser_returns200WithUsageInfo() throws Exception {
        User user = createUser("getsub@example.com");

        mockMvc.perform(get("/api/v1/billing/subscription")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tier").value("FREE"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.provider").value("DEMO"))
                .andExpect(jsonPath("$.pdfExportsUsed").value(0))
                .andExpect(jsonPath("$.pdfExportsLimit").value(3));
    }

    @Test
    void getSubscription_freeUserWithUsage_returnsCorrectCount() throws Exception {
        User user = createUser("getsubusage@example.com");
        usageRepository.save(PdfExportUsage.builder()
                .user(user)
                .billingPeriod(LocalDate.now().withDayOfMonth(1))
                .exportCount(2)
                .build());

        mockMvc.perform(get("/api/v1/billing/subscription")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pdfExportsUsed").value(2))
                .andExpect(jsonPath("$.pdfExportsLimit").value(3));
    }

    @Test
    void getSubscription_proUser_returnsProTierWithNullUsage() throws Exception {
        User user = createUser("getsubpro@example.com");
        billingService.upgrade(user);
        User reloaded = userRepository.findById(user.getId()).orElseThrow();

        mockMvc.perform(get("/api/v1/billing/subscription")
                        .header("Authorization", bearer(reloaded)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tier").value("PRO"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.pdfExportsUsed").doesNotExist())
                .andExpect(jsonPath("$.pdfExportsLimit").doesNotExist());
    }

    @Test
    void getSubscription_includesBillingPeriod() throws Exception {
        User user = createUser("getsubperiod@example.com");
        billingService.upgrade(user);
        User reloaded = userRepository.findById(user.getId()).orElseThrow();

        mockMvc.perform(get("/api/v1/billing/subscription")
                        .header("Authorization", bearer(reloaded)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPeriodStart").isNotEmpty())
                .andExpect(jsonPath("$.currentPeriodEnd").isNotEmpty());
    }

    // ── POST /checkout ────────────────────────────────────────────────────────

    @Test
    void checkout_freeUser_upgradesAndReturns200() throws Exception {
        User user = createUser("checkout@example.com");

        mockMvc.perform(post("/api/v1/billing/checkout")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("UPGRADED"))
                .andExpect(jsonPath("$.tier").value("PRO"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void checkout_alreadyPro_returns409() throws Exception {
        User user = createUser("checkoutpro@example.com");
        billingService.upgrade(user);
        User reloaded = userRepository.findById(user.getId()).orElseThrow();

        mockMvc.perform(post("/api/v1/billing/checkout")
                        .header("Authorization", bearer(reloaded)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_PRO"));
    }

    // ── POST /cancel ──────────────────────────────────────────────────────────

    @Test
    void cancel_proUser_cancelsAndReturns200() throws Exception {
        User user = createUser("cancel@example.com");
        billingService.upgrade(user);
        User reloaded = userRepository.findById(user.getId()).orElseThrow();

        mockMvc.perform(post("/api/v1/billing/cancel")
                        .header("Authorization", bearer(reloaded)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tier").value("FREE"))
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    void cancel_freeUser_noActiveSubscription_returns400() throws Exception {
        // After cancel, subscription is CANCELED (not ACTIVE), so a second cancel
        // finds no active subscription → 400 NO_ACTIVE_SUBSCRIPTION.
        User user = createUser("cancelfree@example.com");
        billingService.upgrade(user);
        User upgraded = userRepository.findById(user.getId()).orElseThrow();
        billingService.cancel(upgraded);
        User canceled = userRepository.findById(user.getId()).orElseThrow();

        mockMvc.perform(post("/api/v1/billing/cancel")
                        .header("Authorization", bearer(canceled)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NO_ACTIVE_SUBSCRIPTION"));
    }

    // ── Cross-user isolation ──────────────────────────────────────────────────

    @Test
    void getSubscription_returnsOnlyOwnSubscription() throws Exception {
        User userA = createUser("isoA@example.com");
        User userB = createUser("isoB@example.com");
        billingService.upgrade(userA);

        // userB must see their own FREE subscription, not userA's PRO
        mockMvc.perform(get("/api/v1/billing/subscription")
                        .header("Authorization", bearer(userB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tier").value("FREE"));
    }

    @Test
    void checkout_upgradesOnlyAuthenticatedUser() throws Exception {
        User userA = createUser("upgradeA@example.com");
        User userB = createUser("upgradeB@example.com");

        // userB upgrades — userA must remain FREE
        mockMvc.perform(post("/api/v1/billing/checkout")
                        .header("Authorization", bearer(userB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tier").value("PRO"));

        mockMvc.perform(get("/api/v1/billing/subscription")
                        .header("Authorization", bearer(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tier").value("FREE"));
    }

    // ── Provider failure ──────────────────────────────────────────────────────

    @Test
    void getSubscription_noActiveSubscription_returns400() throws Exception {
        // User with no subscription record at all
        User user = userRepository.save(User.builder()
                .email("nosub@example.com")
                .passwordHash(passwordEncoder.encode("Password1"))
                .subscriptionTier(SubscriptionTier.FREE)
                .enabled(true)
                .build());

        mockMvc.perform(get("/api/v1/billing/subscription")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NO_ACTIVE_SUBSCRIPTION"));
    }

    // ── Existing features unaffected ──────────────────────────────────────────

    @Test
    void billingEndpoints_doNotAffectHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk());
    }
}
