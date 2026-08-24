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
class AnalyticsApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
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

    private String bearer(User user) {
        return "Bearer " + jwtService.generateAccessToken(user.getId(), user.getEmail());
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

    // ── Test 1: Unauthenticated → 401 ─────────────────────────────────────────

    @Test
    void analytics_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/analytics"))
                .andExpect(status().isUnauthorized());
    }

    // ── Test 2: Empty — no applications ──────────────────────────────────────

    @Test
    void analytics_noApplications_returnsZerosAndEmptyTrend() throws Exception {
        User user = createUser("empty@apianalytics.com");

        mockMvc.perform(get("/api/v1/dashboard/analytics")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pipelineApplied").value(0))
                .andExpect(jsonPath("$.pipelineInterview").value(0))
                .andExpect(jsonPath("$.pipelineOffer").value(0))
                .andExpect(jsonPath("$.pipelineRejected").value(0))
                .andExpect(jsonPath("$.trend").isArray())
                .andExpect(jsonPath("$.trend", hasSize(0)));
    }

    // ── Test 3: Pipeline distribution ────────────────────────────────────────

    @Test
    void analytics_pipelineDistribution_isCorrect() throws Exception {
        User user = createUser("pipeline@apianalytics.com");
        LocalDate today = LocalDate.now();
        createApplication(user, "A", ApplicationStatus.APPLIED,   today);
        createApplication(user, "B", ApplicationStatus.APPLIED,   today);
        createApplication(user, "C", ApplicationStatus.INTERVIEW, today);
        createApplication(user, "D", ApplicationStatus.OFFER,     today);
        createApplication(user, "E", ApplicationStatus.REJECTED,  today);

        mockMvc.perform(get("/api/v1/dashboard/analytics")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pipelineApplied").value(2))
                .andExpect(jsonPath("$.pipelineInterview").value(1))
                .andExpect(jsonPath("$.pipelineOffer").value(1))
                .andExpect(jsonPath("$.pipelineRejected").value(1));
    }

    // ── Test 4: Trend — grouped by month ─────────────────────────────────────

    @Test
    void analytics_trend_groupedByMonth() throws Exception {
        User user = createUser("trend@apianalytics.com");
        LocalDate thisMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate lastMonth = thisMonth.minusMonths(1);

        createApplication(user, "A", ApplicationStatus.APPLIED, thisMonth);
        createApplication(user, "B", ApplicationStatus.APPLIED, thisMonth);
        createApplication(user, "C", ApplicationStatus.APPLIED, lastMonth);

        mockMvc.perform(get("/api/v1/dashboard/analytics")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trend", hasSize(2)))
                .andExpect(jsonPath("$.trend[0].count").value(1))  // last month
                .andExpect(jsonPath("$.trend[1].count").value(2)); // this month
    }

    // ── Test 5: Trend — old data excluded ────────────────────────────────────

    @Test
    void analytics_trend_excludesDataOlderThan12Months() throws Exception {
        User user = createUser("old@apianalytics.com");
        createApplication(user, "Old", ApplicationStatus.APPLIED, LocalDate.now().minusMonths(13));
        createApplication(user, "New", ApplicationStatus.APPLIED, LocalDate.now());

        mockMvc.perform(get("/api/v1/dashboard/analytics")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trend", hasSize(1)))
                .andExpect(jsonPath("$.trend[0].count").value(1));
    }

    // ── Test 6: User isolation ────────────────────────────────────────────────

    @Test
    void analytics_userIsolation_userBSeesOnlyOwnData() throws Exception {
        User userA = createUser("isoA@apianalytics.com");
        User userB = createUser("isoB@apianalytics.com");
        LocalDate today = LocalDate.now();

        createApplication(userA, "A", ApplicationStatus.APPLIED,   today);
        createApplication(userA, "B", ApplicationStatus.INTERVIEW, today);
        createApplication(userB, "C", ApplicationStatus.OFFER,     today);

        mockMvc.perform(get("/api/v1/dashboard/analytics")
                        .header("Authorization", bearer(userB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pipelineOffer").value(1))
                .andExpect(jsonPath("$.pipelineApplied").value(0))
                .andExpect(jsonPath("$.pipelineInterview").value(0))
                .andExpect(jsonPath("$.trend[0].count").value(1));
    }

    // ── Test 7: Response schema ───────────────────────────────────────────────

    @Test
    void analytics_responseSchema_containsAllFields() throws Exception {
        User user = createUser("schema@apianalytics.com");

        mockMvc.perform(get("/api/v1/dashboard/analytics")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pipelineApplied").exists())
                .andExpect(jsonPath("$.pipelineInterview").exists())
                .andExpect(jsonPath("$.pipelineOffer").exists())
                .andExpect(jsonPath("$.pipelineRejected").exists())
                .andExpect(jsonPath("$.trend").exists());
    }
}
