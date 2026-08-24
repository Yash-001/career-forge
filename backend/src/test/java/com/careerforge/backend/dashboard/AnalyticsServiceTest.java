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
import com.careerforge.backend.dashboard.dto.AnalyticsSummary;
import com.careerforge.backend.dashboard.dto.ApplicationTrendEntry;
import com.careerforge.backend.dashboard.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AnalyticsServiceTest extends AbstractIntegrationTest {

    @Autowired AnalyticsService analyticsService;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired SubscriptionService subscriptionService;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired ApplicationRepository applicationRepository;

    @BeforeEach
    void clean() {
        applicationRepository.deleteAll();
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

    private Application createApplication(User user, String company, ApplicationStatus status, LocalDate date) {
        return applicationRepository.save(Application.builder()
                .user(user)
                .companyName(company)
                .jobTitle("Engineer")
                .applicationDate(date)
                .status(status)
                .build());
    }

    // ── Test 1: Empty — no applications ──────────────────────────────────────

    @Test
    void noApplications_returnsZeroPipelineAndEmptyTrend() {
        User user = createUser("empty@analytics.com");

        AnalyticsSummary result = analyticsService.getAnalytics(user);

        assertThat(result.pipelineApplied()).isZero();
        assertThat(result.pipelineInterview()).isZero();
        assertThat(result.pipelineOffer()).isZero();
        assertThat(result.pipelineRejected()).isZero();
        assertThat(result.trend()).isEmpty();
    }

    // ── Test 2: Pipeline distribution correctness ─────────────────────────────

    @Test
    void pipelineDistribution_countsEachStatusCorrectly() {
        User user = createUser("pipeline@analytics.com");
        LocalDate today = LocalDate.now();
        createApplication(user, "A", ApplicationStatus.APPLIED,   today);
        createApplication(user, "B", ApplicationStatus.APPLIED,   today);
        createApplication(user, "C", ApplicationStatus.INTERVIEW, today);
        createApplication(user, "D", ApplicationStatus.OFFER,     today);
        createApplication(user, "E", ApplicationStatus.REJECTED,  today);
        createApplication(user, "F", ApplicationStatus.REJECTED,  today);

        AnalyticsSummary result = analyticsService.getAnalytics(user);

        assertThat(result.pipelineApplied()).isEqualTo(2);
        assertThat(result.pipelineInterview()).isEqualTo(1);
        assertThat(result.pipelineOffer()).isEqualTo(1);
        assertThat(result.pipelineRejected()).isEqualTo(2);
    }

    // ── Test 3: Trend — single month ─────────────────────────────────────────

    @Test
    void trend_singleMonth_returnsSingleEntry() {
        User user = createUser("trend1@analytics.com");
        LocalDate today = LocalDate.now();
        createApplication(user, "A", ApplicationStatus.APPLIED, today);
        createApplication(user, "B", ApplicationStatus.APPLIED, today);

        AnalyticsSummary result = analyticsService.getAnalytics(user);

        assertThat(result.trend()).hasSize(1);
        ApplicationTrendEntry entry = result.trend().get(0);
        assertThat(entry.year()).isEqualTo(today.getYear());
        assertThat(entry.month()).isEqualTo(today.getMonthValue());
        assertThat(entry.count()).isEqualTo(2);
    }

    // ── Test 4: Trend — multiple months grouped correctly ─────────────────────

    @Test
    void trend_multipleMonths_groupedByYearMonth() {
        User user = createUser("trend2@analytics.com");
        LocalDate thisMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate lastMonth = thisMonth.minusMonths(1);
        LocalDate twoMonthsAgo = thisMonth.minusMonths(2);

        createApplication(user, "A", ApplicationStatus.APPLIED, thisMonth);
        createApplication(user, "B", ApplicationStatus.APPLIED, thisMonth);
        createApplication(user, "C", ApplicationStatus.APPLIED, lastMonth);
        createApplication(user, "D", ApplicationStatus.APPLIED, twoMonthsAgo);
        createApplication(user, "E", ApplicationStatus.APPLIED, twoMonthsAgo);
        createApplication(user, "F", ApplicationStatus.APPLIED, twoMonthsAgo);

        AnalyticsSummary result = analyticsService.getAnalytics(user);

        assertThat(result.trend()).hasSize(3);
        // Ordered ascending by date
        assertThat(result.trend().get(0).count()).isEqualTo(3); // two months ago
        assertThat(result.trend().get(1).count()).isEqualTo(1); // last month
        assertThat(result.trend().get(2).count()).isEqualTo(2); // this month
    }

    // ── Test 5: Trend — data older than 12 months is excluded ─────────────────

    @Test
    void trend_dataOlderThan12Months_isExcluded() {
        User user = createUser("trend3@analytics.com");
        LocalDate today = LocalDate.now();
        LocalDate old = today.minusMonths(13);

        createApplication(user, "Old", ApplicationStatus.APPLIED, old);
        createApplication(user, "New", ApplicationStatus.APPLIED, today);

        AnalyticsSummary result = analyticsService.getAnalytics(user);

        // Only the current month entry should appear
        assertThat(result.trend()).hasSize(1);
        assertThat(result.trend().get(0).year()).isEqualTo(today.getYear());
        assertThat(result.trend().get(0).month()).isEqualTo(today.getMonthValue());
        assertThat(result.trend().get(0).count()).isEqualTo(1);
    }

    // ── Test 6: User isolation ────────────────────────────────────────────────

    @Test
    void userIsolation_userBSeesOnlyOwnData() {
        User userA = createUser("isoA@analytics.com");
        User userB = createUser("isoB@analytics.com");
        LocalDate today = LocalDate.now();

        createApplication(userA, "A Corp", ApplicationStatus.APPLIED,   today);
        createApplication(userA, "B Corp", ApplicationStatus.INTERVIEW, today);
        createApplication(userB, "C Corp", ApplicationStatus.OFFER,     today);

        AnalyticsSummary resultA = analyticsService.getAnalytics(userA);
        AnalyticsSummary resultB = analyticsService.getAnalytics(userB);

        assertThat(resultA.pipelineApplied()).isEqualTo(1);
        assertThat(resultA.pipelineInterview()).isEqualTo(1);
        assertThat(resultA.pipelineOffer()).isZero();

        assertThat(resultB.pipelineOffer()).isEqualTo(1);
        assertThat(resultB.pipelineApplied()).isZero();
        assertThat(resultB.pipelineInterview()).isZero();

        assertThat(resultA.trend().stream().mapToLong(ApplicationTrendEntry::count).sum()).isEqualTo(2);
        assertThat(resultB.trend().stream().mapToLong(ApplicationTrendEntry::count).sum()).isEqualTo(1);
    }
}
