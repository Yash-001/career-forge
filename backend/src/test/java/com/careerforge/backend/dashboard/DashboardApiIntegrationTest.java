package com.careerforge.backend.dashboard;

import com.careerforge.backend.AbstractIntegrationTest;
import com.careerforge.backend.application.domain.Application;
import com.careerforge.backend.application.domain.ApplicationStatus;
import com.careerforge.backend.application.repository.ApplicationRepository;
import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.auth.repository.UserRepository;
import com.careerforge.backend.billing.BillingService;
import com.careerforge.backend.billing.SubscriptionRepository;
import com.careerforge.backend.billing.SubscriptionService;
import com.careerforge.backend.pdf.domain.PdfExportUsage;
import com.careerforge.backend.pdf.repository.PdfExportUsageRepository;
import com.careerforge.backend.profile.domain.MasterProfile;
import com.careerforge.backend.profile.repository.MasterProfileRepository;
import com.careerforge.backend.resume.domain.Resume;
import com.careerforge.backend.resume.domain.ResumeVersion;
import com.careerforge.backend.resume.repository.ResumeRepository;
import com.careerforge.backend.resume.repository.ResumeVersionRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class DashboardApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    @Autowired SubscriptionService subscriptionService;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired BillingService billingService;
    @Autowired MasterProfileRepository profileRepository;
    @Autowired ResumeRepository resumeRepository;
    @Autowired ResumeVersionRepository resumeVersionRepository;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired PdfExportUsageRepository pdfExportUsageRepository;

    @BeforeEach
    void clean() {
        pdfExportUsageRepository.deleteAll();
        applicationRepository.deleteAll();
        resumeVersionRepository.deleteAll();
        resumeRepository.deleteAll();
        profileRepository.deleteAll();
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

    private Resume createResume(User user, String name) {
        Resume resume = resumeRepository.save(Resume.builder().user(user).name(name).build());
        resumeVersionRepository.save(ResumeVersion.builder()
                .resume(resume).versionNumber(1).title("v1").build());
        return resume;
    }

    private Application createApplication(User user, String company, ApplicationStatus status) {
        return applicationRepository.save(Application.builder()
                .user(user)
                .companyName(company)
                .jobTitle("Engineer")
                .applicationDate(LocalDate.now())
                .status(status)
                .build());
    }

    // ── Test 1: Unauthenticated → 401 ─────────────────────────────────────────

    @Test
    void dashboard_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    // ── Test 2: Authenticated empty dashboard → 200 ───────────────────────────

    @Test
    void dashboard_newUser_returns200WithEmptyData() throws Exception {
        User user = createUser("empty@example.com");

        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.exists").value(false))
                .andExpect(jsonPath("$.profile.completionPercent").value(0))
                .andExpect(jsonPath("$.resumes.resumeCount").value(0))
                .andExpect(jsonPath("$.resumes.versionCount").value(0))
                .andExpect(jsonPath("$.resumes.recentResumes").isArray())
                .andExpect(jsonPath("$.resumes.recentResumes", hasSize(0)))
                .andExpect(jsonPath("$.applications.total").value(0))
                .andExpect(jsonPath("$.applications.recentApplications").isArray())
                .andExpect(jsonPath("$.applications.recentApplications", hasSize(0)))
                .andExpect(jsonPath("$.usage.pdfExportsUsed").value(0))
                .andExpect(jsonPath("$.quickActions.canLogApplication").value(true));
    }

    // ── Test 3: Populated dashboard ───────────────────────────────────────────

    @Test
    void dashboard_populatedUser_returnsCorrectData() throws Exception {
        User user = createUser("populated@example.com");
        profileRepository.save(MasterProfile.builder()
                .user(user)
                .professionalTitle("Engineer")
                .professionalSummary("Summary")
                .phone("555-0000")
                .build());
        createResume(user, "Resume A");
        createResume(user, "Resume B");
        createApplication(user, "Acme", ApplicationStatus.APPLIED);

        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.exists").value(true))
                .andExpect(jsonPath("$.profile.completionPercent").value(100))
                .andExpect(jsonPath("$.resumes.resumeCount").value(2))
                .andExpect(jsonPath("$.applications.total").value(1));
    }

    // ── Test 4: Application status aggregation ────────────────────────────────

    @Test
    void dashboard_applicationStatusCounts_areCorrect() throws Exception {
        User user = createUser("statuses@example.com");
        createApplication(user, "A", ApplicationStatus.APPLIED);
        createApplication(user, "B", ApplicationStatus.APPLIED);
        createApplication(user, "C", ApplicationStatus.INTERVIEW);
        createApplication(user, "D", ApplicationStatus.OFFER);
        createApplication(user, "E", ApplicationStatus.REJECTED);

        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applications.total").value(5))
                .andExpect(jsonPath("$.applications.applied").value(2))
                .andExpect(jsonPath("$.applications.interview").value(1))
                .andExpect(jsonPath("$.applications.offer").value(1))
                .andExpect(jsonPath("$.applications.rejected").value(1));
    }

    // ── Test 5: Recent applications bounded to 5 ─────────────────────────────

    @Test
    void dashboard_recentApplications_boundedToFive() throws Exception {
        User user = createUser("recentapps@example.com");
        for (int i = 1; i <= 7; i++) {
            createApplication(user, "Company " + i, ApplicationStatus.APPLIED);
        }

        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applications.total").value(7))
                .andExpect(jsonPath("$.applications.recentApplications", hasSize(5)));
    }

    // ── Test 6: Subscription information ─────────────────────────────────────

    @Test
    void dashboard_freeUser_subscriptionTierIsFree() throws Exception {
        User user = createUser("subtier@example.com");

        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscription.tier").value("FREE"))
                .andExpect(jsonPath("$.subscription.status").value("ACTIVE"));
    }

    @Test
    void dashboard_proUser_subscriptionTierIsPro() throws Exception {
        User user = createUser("subpro@example.com");
        billingService.upgrade(user);
        User reloaded = userRepository.findById(user.getId()).orElseThrow();

        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", bearer(reloaded)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscription.tier").value("PRO"));
    }

    // ── Test 7: PDF usage ─────────────────────────────────────────────────────

    @Test
    void dashboard_freeUser_pdfUsageReflectsExports() throws Exception {
        User user = createUser("pdfusage@example.com");
        pdfExportUsageRepository.save(PdfExportUsage.builder()
                .user(user)
                .billingPeriod(LocalDate.now().withDayOfMonth(1))
                .exportCount(2)
                .build());

        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usage.pdfExportsUsed").value(2))
                .andExpect(jsonPath("$.usage.pdfExportsLimit").value(3))
                .andExpect(jsonPath("$.usage.atLimit").value(false));
    }

    @Test
    void dashboard_freeUser_atLimit_atLimitIsTrue() throws Exception {
        User user = createUser("atlimit@example.com");
        pdfExportUsageRepository.save(PdfExportUsage.builder()
                .user(user)
                .billingPeriod(LocalDate.now().withDayOfMonth(1))
                .exportCount(3)
                .build());

        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usage.atLimit").value(true));
    }

    // ── Test 8: User isolation ────────────────────────────────────────────────

    @Test
    void dashboard_userIsolation_userBSeesOnlyOwnData() throws Exception {
        User userA = createUser("isoA@example.com");
        User userB = createUser("isoB@example.com");

        profileRepository.save(MasterProfile.builder()
                .user(userA).professionalTitle("Engineer").build());
        createResume(userA, "UserA Resume");
        createApplication(userA, "UserA Corp", ApplicationStatus.APPLIED);

        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", bearer(userB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.exists").value(false))
                .andExpect(jsonPath("$.resumes.resumeCount").value(0))
                .andExpect(jsonPath("$.applications.total").value(0));
    }

    // ── Test 9: Response schema ───────────────────────────────────────────────

    @Test
    void dashboard_responseSchema_containsAllTopLevelFields() throws Exception {
        User user = createUser("schema@example.com");

        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile").exists())
                .andExpect(jsonPath("$.resumes").exists())
                .andExpect(jsonPath("$.applications").exists())
                .andExpect(jsonPath("$.subscription").exists())
                .andExpect(jsonPath("$.usage").exists())
                .andExpect(jsonPath("$.quickActions").exists())
                .andExpect(jsonPath("$.analytics").exists())
                .andExpect(jsonPath("$.activity").isArray());
    }

    @Test
    void dashboard_responseSchema_noStripeSecretsLeaked() throws Exception {
        User user = createUser("nosecrets@example.com");

        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscription.providerCustomerId").doesNotExist())
                .andExpect(jsonPath("$.subscription.providerSubscriptionId").doesNotExist());
    }

    // ── Test 10: Quick actions ────────────────────────────────────────────────

    @Test
    void dashboard_quickActions_freeUserUnderLimit_canCreateAndUpgrade() throws Exception {
        User user = createUser("qa@example.com");

        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quickActions.canCreateResume").value(true))
                .andExpect(jsonPath("$.quickActions.canLogApplication").value(true))
                .andExpect(jsonPath("$.quickActions.canUpgrade").value(true));
    }

    @Test
    void dashboard_quickActions_proUser_cannotUpgrade() throws Exception {
        User user = createUser("qapro@example.com");
        billingService.upgrade(user);
        User reloaded = userRepository.findById(user.getId()).orElseThrow();

        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", bearer(reloaded)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quickActions.canUpgrade").value(false))
                .andExpect(jsonPath("$.quickActions.canCreateResume").value(true));
    }
}
