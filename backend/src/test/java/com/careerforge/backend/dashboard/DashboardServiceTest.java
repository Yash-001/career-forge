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
import com.careerforge.backend.dashboard.dto.*;
import com.careerforge.backend.dashboard.service.DashboardService;
import com.careerforge.backend.pdf.domain.PdfExportUsage;
import com.careerforge.backend.pdf.repository.PdfExportUsageRepository;
import com.careerforge.backend.profile.domain.MasterProfile;
import com.careerforge.backend.profile.repository.MasterProfileRepository;
import com.careerforge.backend.resume.domain.Resume;
import com.careerforge.backend.resume.domain.ResumeVersion;
import com.careerforge.backend.resume.repository.ResumeRepository;
import com.careerforge.backend.resume.repository.ResumeVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DashboardServiceTest extends AbstractIntegrationTest {

    @Autowired DashboardService dashboardService;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
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

    // ── Test 1: New user with no data ─────────────────────────────────────────

    @Test
    void newUser_noData_returnsEmptyDashboard() {
        User user = createUser("newuser@example.com");

        DashboardSummary summary = dashboardService.getDashboard(user);

        assertThat(summary.profile().exists()).isFalse();
        assertThat(summary.profile().completionPercent()).isEqualTo(0);
        assertThat(summary.resumes().resumeCount()).isEqualTo(0);
        assertThat(summary.resumes().versionCount()).isEqualTo(0);
        assertThat(summary.resumes().recentResumes()).isEmpty();
        assertThat(summary.applications().total()).isEqualTo(0);
        assertThat(summary.applications().recentApplications()).isEmpty();
        assertThat(summary.subscription()).isNotNull();
        assertThat(summary.usage().pdfExportsUsed()).isEqualTo(0);
        assertThat(summary.quickActions().canLogApplication()).isTrue();
    }

    // ── Test 2: User with profile ─────────────────────────────────────────────

    @Test
    void userWithProfile_profileSummaryReflectsFields() {
        User user = createUser("profile@example.com");
        profileRepository.save(MasterProfile.builder()
                .user(user)
                .professionalTitle("Software Engineer")
                .professionalSummary("Experienced developer")
                .phone("555-1234")
                .build());

        ProfileSummary profile = dashboardService.getDashboard(user).profile();

        assertThat(profile.exists()).isTrue();
        assertThat(profile.hasTitle()).isTrue();
        assertThat(profile.hasSummary()).isTrue();
        assertThat(profile.hasContactInfo()).isTrue();
        assertThat(profile.completionPercent()).isEqualTo(100);
    }

    @Test
    void userWithPartialProfile_completionIsPartial() {
        User user = createUser("partial@example.com");
        profileRepository.save(MasterProfile.builder()
                .user(user)
                .professionalTitle("Engineer")
                .build());

        ProfileSummary profile = dashboardService.getDashboard(user).profile();

        assertThat(profile.exists()).isTrue();
        assertThat(profile.hasTitle()).isTrue();
        assertThat(profile.hasSummary()).isFalse();
        assertThat(profile.hasContactInfo()).isFalse();
        assertThat(profile.completionPercent()).isEqualTo(50);
    }

    // ── Test 3: User with multiple resumes ────────────────────────────────────

    @Test
    void userWithMultipleResumes_resumeCountIsCorrect() {
        User user = createUser("resumes@example.com");
        createResume(user, "Resume A");
        createResume(user, "Resume B");
        createResume(user, "Resume C");

        ResumeSummary resumes = dashboardService.getDashboard(user).resumes();

        assertThat(resumes.resumeCount()).isEqualTo(3);
        assertThat(resumes.recentResumes()).hasSize(3);
    }

    // ── Test 4: User with multiple resume versions ────────────────────────────

    @Test
    void userWithMultipleVersions_versionCountIsCorrect() {
        User user = createUser("versions@example.com");
        Resume resume = createResume(user, "My Resume");
        // createResume already adds version 1; add two more
        resumeVersionRepository.save(ResumeVersion.builder()
                .resume(resume).versionNumber(2).title("v2").build());
        resumeVersionRepository.save(ResumeVersion.builder()
                .resume(resume).versionNumber(3).title("v3").build());

        ResumeSummary resumes = dashboardService.getDashboard(user).resumes();

        assertThat(resumes.resumeCount()).isEqualTo(1);
        assertThat(resumes.versionCount()).isEqualTo(3);
    }

    // ── Test 5: User with applications ───────────────────────────────────────

    @Test
    void userWithApplications_totalCountIsCorrect() {
        User user = createUser("apps@example.com");
        createApplication(user, "Acme", ApplicationStatus.APPLIED);
        createApplication(user, "Beta", ApplicationStatus.INTERVIEW);
        createApplication(user, "Gamma", ApplicationStatus.OFFER);

        ApplicationSummary apps = dashboardService.getDashboard(user).applications();

        assertThat(apps.total()).isEqualTo(3);
    }

    // ── Test 6: Application status aggregation ────────────────────────────────

    @Test
    void applicationStatusCounts_areCorrect() {
        User user = createUser("statuses@example.com");
        createApplication(user, "A", ApplicationStatus.APPLIED);
        createApplication(user, "B", ApplicationStatus.APPLIED);
        createApplication(user, "C", ApplicationStatus.INTERVIEW);
        createApplication(user, "D", ApplicationStatus.OFFER);
        createApplication(user, "E", ApplicationStatus.REJECTED);

        ApplicationSummary apps = dashboardService.getDashboard(user).applications();

        assertThat(apps.applied()).isEqualTo(2);
        assertThat(apps.interview()).isEqualTo(1);
        assertThat(apps.offer()).isEqualTo(1);
        assertThat(apps.rejected()).isEqualTo(1);
        assertThat(apps.total()).isEqualTo(5);
    }

    // ── Test 7: Recent applications bounded to 5 ─────────────────────────────

    @Test
    void recentApplications_boundedToFive() {
        User user = createUser("recent@example.com");
        for (int i = 1; i <= 7; i++) {
            createApplication(user, "Company " + i, ApplicationStatus.APPLIED);
        }

        ApplicationSummary apps = dashboardService.getDashboard(user).applications();

        assertThat(apps.total()).isEqualTo(7);
        assertThat(apps.recentApplications()).hasSize(5);
    }

    // ── Test 8: Subscription information ─────────────────────────────────────

    @Test
    void freeUser_subscriptionSummaryIsFree() {
        User user = createUser("sub@example.com");

        SubscriptionSummary sub = dashboardService.getDashboard(user).subscription();

        assertThat(sub.tier()).isEqualTo(SubscriptionTier.FREE);
    }

    @Test
    void proUser_subscriptionSummaryIsPro() {
        User user = createUser("subpro@example.com");
        billingService.upgrade(user);
        User reloaded = userRepository.findById(user.getId()).orElseThrow();

        SubscriptionSummary sub = dashboardService.getDashboard(reloaded).subscription();

        assertThat(sub.tier()).isEqualTo(SubscriptionTier.PRO);
    }

    // ── Test 9: PDF usage ─────────────────────────────────────────────────────

    @Test
    void freeUser_usageSummaryReflectsExports() {
        User user = createUser("usage@example.com");
        pdfExportUsageRepository.save(PdfExportUsage.builder()
                .user(user)
                .billingPeriod(LocalDate.now().withDayOfMonth(1))
                .exportCount(2)
                .build());

        UsageSummary usage = dashboardService.getDashboard(user).usage();

        assertThat(usage.pdfExportsUsed()).isEqualTo(2);
        assertThat(usage.pdfExportsLimit()).isEqualTo(3);
        assertThat(usage.atLimit()).isFalse();
    }

    @Test
    void freeUser_atLimit_atLimitIsTrue() {
        User user = createUser("atlimit@example.com");
        pdfExportUsageRepository.save(PdfExportUsage.builder()
                .user(user)
                .billingPeriod(LocalDate.now().withDayOfMonth(1))
                .exportCount(3)
                .build());

        UsageSummary usage = dashboardService.getDashboard(user).usage();

        assertThat(usage.atLimit()).isTrue();
    }

    @Test
    void proUser_usageSummaryShowsZeroLimits() {
        User user = createUser("prousage@example.com");
        billingService.upgrade(user);
        User reloaded = userRepository.findById(user.getId()).orElseThrow();

        UsageSummary usage = dashboardService.getDashboard(reloaded).usage();

        assertThat(usage.pdfExportsLimit()).isEqualTo(0);
        assertThat(usage.atLimit()).isFalse();
    }

    // ── Test 10: User isolation ───────────────────────────────────────────────

    @Test
    void userIsolation_userADataNotVisibleToUserB() {
        User userA = createUser("isoA@example.com");
        User userB = createUser("isoB@example.com");

        createResume(userA, "UserA Resume");
        createApplication(userA, "UserA Corp", ApplicationStatus.APPLIED);
        profileRepository.save(MasterProfile.builder()
                .user(userA).professionalTitle("Engineer").build());

        DashboardSummary summaryB = dashboardService.getDashboard(userB);

        assertThat(summaryB.profile().exists()).isFalse();
        assertThat(summaryB.resumes().resumeCount()).isEqualTo(0);
        assertThat(summaryB.applications().total()).isEqualTo(0);
    }

    // ── Test 11: Empty/null optional data ─────────────────────────────────────

    @Test
    void noProfile_profileSummaryHasZeroCompletion() {
        User user = createUser("noprofile@example.com");

        ProfileSummary profile = dashboardService.getDashboard(user).profile();

        assertThat(profile.exists()).isFalse();
        assertThat(profile.completionPercent()).isEqualTo(0);
        assertThat(profile.hasTitle()).isFalse();
        assertThat(profile.hasSummary()).isFalse();
        assertThat(profile.hasContactInfo()).isFalse();
    }

    @Test
    void noExports_usageSummaryShowsZero() {
        User user = createUser("noexports@example.com");

        UsageSummary usage = dashboardService.getDashboard(user).usage();

        assertThat(usage.pdfExportsUsed()).isEqualTo(0);
        assertThat(usage.atLimit()).isFalse();
    }

    // ── Test 12: Quick actions ────────────────────────────────────────────────

    @Test
    void freeUserUnderLimit_canCreateResume() {
        User user = createUser("qa@example.com");

        QuickActions qa = dashboardService.getDashboard(user).quickActions();

        assertThat(qa.canCreateResume()).isTrue();
        assertThat(qa.canLogApplication()).isTrue();
        assertThat(qa.canUpgrade()).isTrue();
    }

    @Test
    void freeUserAtResumeLimit_cannotCreateResume() {
        User user = createUser("qalimit@example.com");
        createResume(user, "Resume 1");
        createResume(user, "Resume 2");

        QuickActions qa = dashboardService.getDashboard(user).quickActions();

        assertThat(qa.canCreateResume()).isFalse();
        assertThat(qa.canUpgrade()).isTrue();
    }

    @Test
    void proUser_canCreateResumeAndCannotUpgrade() {
        User user = createUser("qapro@example.com");
        billingService.upgrade(user);
        User reloaded = userRepository.findById(user.getId()).orElseThrow();

        QuickActions qa = dashboardService.getDashboard(reloaded).quickActions();

        assertThat(qa.canCreateResume()).isTrue();
        assertThat(qa.canUpgrade()).isFalse();
    }

    // ── Test 13: Recent resumes bounded to 5 ─────────────────────────────────

    @Test
    void recentResumes_boundedToFive() {
        User user = createUser("recentresumes@example.com");
        for (int i = 1; i <= 7; i++) {
            createResume(user, "Resume " + i);
        }

        ResumeSummary resumes = dashboardService.getDashboard(user).resumes();

        assertThat(resumes.resumeCount()).isEqualTo(7);
        assertThat(resumes.recentResumes()).hasSize(5);
    }
}
