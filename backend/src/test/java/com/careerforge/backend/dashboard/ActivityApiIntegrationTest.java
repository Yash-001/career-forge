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
class ActivityApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    @Autowired SubscriptionService subscriptionService;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired ResumeRepository resumeRepository;
    @Autowired ResumeVersionRepository resumeVersionRepository;
    @Autowired ApplicationRepository applicationRepository;

    @BeforeEach
    void clean() {
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

    private String bearer(User user) {
        return "Bearer " + jwtService.generateAccessToken(user.getId(), user.getEmail());
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

    // ── Test 1: Unauthenticated → 401 ─────────────────────────────────────────

    @Test
    void activity_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/activity"))
                .andExpect(status().isUnauthorized());
    }

    // ── Test 2: Empty feed ────────────────────────────────────────────────────

    @Test
    void activity_noData_returnsEmptyArray() throws Exception {
        User user = createUser("empty@apiactivity.com");

        mockMvc.perform(get("/api/v1/dashboard/activity")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ── Test 3: Resume activity appears ──────────────────────────────────────

    @Test
    void activity_resumeCreated_appearsInFeed() throws Exception {
        User user = createUser("resume@apiactivity.com");
        createResume(user, "My Resume");

        mockMvc.perform(get("/api/v1/dashboard/activity")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[*].type", hasItem("RESUME_UPDATED")));
    }

    // ── Test 4: Application activity appears ─────────────────────────────────

    @Test
    void activity_applicationAdded_appearsInFeed() throws Exception {
        User user = createUser("app@apiactivity.com");
        createApplication(user, "Acme Corp");

        mockMvc.perform(get("/api/v1/dashboard/activity")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].type", hasItem("APPLICATION_ADDED")));
    }

    // ── Test 5: Feed bounded to 10 ───────────────────────────────────────────

    @Test
    void activity_bounded_toTenEntries() throws Exception {
        User user = createUser("bounded@apiactivity.com");
        for (int i = 1; i <= 6; i++) {
            createResume(user, "Resume " + i);
        }

        mockMvc.perform(get("/api/v1/dashboard/activity")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(lessThanOrEqualTo(10))));
    }

    // ── Test 6: User isolation ────────────────────────────────────────────────

    @Test
    void activity_userIsolation_userBSeesOnlyOwnData() throws Exception {
        User userA = createUser("isoA@apiactivity.com");
        User userB = createUser("isoB@apiactivity.com");

        createResume(userA, "UserA Resume");
        createApplication(userB, "UserB Corp");

        mockMvc.perform(get("/api/v1/dashboard/activity")
                        .header("Authorization", bearer(userB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].subLabel", not(hasItem(containsString("UserA")))));
    }

    // ── Test 7: Schema fields present ────────────────────────────────────────

    @Test
    void activity_responseSchema_hasRequiredFields() throws Exception {
        User user = createUser("schema@apiactivity.com");
        createResume(user, "Schema Resume");

        mockMvc.perform(get("/api/v1/dashboard/activity")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").exists())
                .andExpect(jsonPath("$[0].label").exists())
                .andExpect(jsonPath("$[0].subLabel").exists())
                .andExpect(jsonPath("$[0].linkPath").exists())
                .andExpect(jsonPath("$[0].occurredAt").exists());
    }
}
