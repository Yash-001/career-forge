package com.careerforge.backend.dashboard;

import com.careerforge.backend.AbstractIntegrationTest;
import com.careerforge.backend.application.domain.Application;
import com.careerforge.backend.application.domain.ApplicationStatus;
import com.careerforge.backend.application.repository.ApplicationRepository;
import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.auth.repository.UserRepository;
import com.careerforge.backend.billing.SubscriptionRepository;
import com.careerforge.backend.billing.SubscriptionService;
import com.careerforge.backend.dashboard.dto.RecentActivityEntry;
import com.careerforge.backend.dashboard.service.ActivityService;
import com.careerforge.backend.pdf.domain.PdfExportUsage;
import com.careerforge.backend.pdf.repository.PdfExportUsageRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ActivityServiceTest extends AbstractIntegrationTest {

    @Autowired ActivityService activityService;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired SubscriptionService subscriptionService;
    @Autowired SubscriptionRepository subscriptionRepository;
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
        subscriptionRepository.deleteAll();
        userRepository.deleteAll();
    }

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

    private Application createApplication(User user, String company) {
        return applicationRepository.save(Application.builder()
                .user(user).companyName(company).jobTitle("Engineer")
                .applicationDate(LocalDate.now()).status(ApplicationStatus.APPLIED).build());
    }

    // ── Test 1: No data → empty feed ──────────────────────────────────────────

    @Test
    void noData_returnsEmptyFeed() {
        User user = createUser("empty@activity.com");
        List<RecentActivityEntry> feed = activityService.getRecentActivity(user);
        assertThat(feed).isEmpty();
    }

    // ── Test 2: Resume update appears in feed ─────────────────────────────────

    @Test
    void resumeUpdate_appearsInFeed() {
        User user = createUser("resume@activity.com");
        createResume(user, "My Resume");

        List<RecentActivityEntry> feed = activityService.getRecentActivity(user);

        assertThat(feed).isNotEmpty();
        assertThat(feed).anyMatch(e -> e.type().equals("RESUME_UPDATED") && e.subLabel().equals("My Resume"));
    }

    // ── Test 3: Application added appears in feed ─────────────────────────────

    @Test
    void applicationAdded_appearsInFeed() {
        User user = createUser("app@activity.com");
        createApplication(user, "Acme Corp");

        List<RecentActivityEntry> feed = activityService.getRecentActivity(user);

        assertThat(feed).anyMatch(e -> e.type().equals("APPLICATION_ADDED") && e.label().contains("Acme Corp"));
    }

    // ── Test 4: Version created appears in feed ───────────────────────────────

    @Test
    void versionCreated_appearsInFeed() {
        User user = createUser("version@activity.com");
        Resume resume = createResume(user, "Tech Resume");
        resumeVersionRepository.save(ResumeVersion.builder()
                .resume(resume).versionNumber(2).title("v2").build());

        List<RecentActivityEntry> feed = activityService.getRecentActivity(user);

        assertThat(feed).anyMatch(e -> e.type().equals("VERSION_CREATED") && e.subLabel().equals("Tech Resume"));
    }

    // ── Test 5: Feed is ordered most-recent first ─────────────────────────────

    @Test
    void feed_isOrderedMostRecentFirst() {
        User user = createUser("order@activity.com");
        createResume(user, "Resume A");
        createApplication(user, "Beta Inc");

        List<RecentActivityEntry> feed = activityService.getRecentActivity(user);

        for (int i = 0; i < feed.size() - 1; i++) {
            assertThat(feed.get(i).occurredAt())
                    .isAfterOrEqualTo(feed.get(i + 1).occurredAt());
        }
    }

    // ── Test 6: Feed bounded to 10 entries ───────────────────────────────────

    @Test
    void feed_boundedToTenEntries() {
        User user = createUser("bounded@activity.com");
        // Create 6 resumes (each triggers RESUME_UPDATED + VERSION_CREATED = 12 events)
        for (int i = 1; i <= 6; i++) {
            createResume(user, "Resume " + i);
        }

        List<RecentActivityEntry> feed = activityService.getRecentActivity(user);

        assertThat(feed).hasSizeLessThanOrEqualTo(10);
    }

    // ── Test 7: User isolation ────────────────────────────────────────────────

    @Test
    void userIsolation_userBSeesOnlyOwnActivity() {
        User userA = createUser("isoA@activity.com");
        User userB = createUser("isoB@activity.com");

        createResume(userA, "UserA Resume");
        createApplication(userA, "UserA Corp");
        createApplication(userB, "UserB Corp");

        List<RecentActivityEntry> feedA = activityService.getRecentActivity(userA);
        List<RecentActivityEntry> feedB = activityService.getRecentActivity(userB);

        assertThat(feedA).noneMatch(e -> e.label().contains("UserB"));
        assertThat(feedB).noneMatch(e -> e.label().contains("UserA") || e.subLabel().contains("UserA"));
    }

    // ── Test 8: linkPath is set correctly ────────────────────────────────────

    @Test
    void resumeActivity_linkPathPointsToResumeEditor() {
        User user = createUser("link@activity.com");
        Resume resume = createResume(user, "Link Resume");

        List<RecentActivityEntry> feed = activityService.getRecentActivity(user);

        assertThat(feed).anyMatch(e ->
                e.type().equals("RESUME_UPDATED") &&
                e.linkPath().equals("/resumes/" + resume.getId()));
    }

    @Test
    void applicationActivity_linkPathPointsToApplications() {
        User user = createUser("applink@activity.com");
        createApplication(user, "Corp X");

        List<RecentActivityEntry> feed = activityService.getRecentActivity(user);

        assertThat(feed).anyMatch(e ->
                e.type().equals("APPLICATION_ADDED") &&
                e.linkPath().equals("/applications"));
    }
}
