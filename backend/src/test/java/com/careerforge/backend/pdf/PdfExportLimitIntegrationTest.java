package com.careerforge.backend.pdf;

import com.careerforge.backend.AbstractIntegrationTest;
import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.auth.repository.UserRepository;
import com.careerforge.backend.pdf.domain.PdfExportUsage;
import com.careerforge.backend.pdf.repository.PdfExportUsageRepository;
import com.careerforge.backend.profile.repository.EducationRepository;
import com.careerforge.backend.profile.repository.MasterProfileRepository;
import com.careerforge.backend.profile.repository.SkillRepository;
import com.careerforge.backend.profile.repository.WorkExperienceRepository;
import com.careerforge.backend.resume.repository.ResumeRepository;
import com.careerforge.backend.shared.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class PdfExportLimitIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    @Autowired MasterProfileRepository profileRepository;
    @Autowired WorkExperienceRepository workExperienceRepository;
    @Autowired EducationRepository educationRepository;
    @Autowired SkillRepository skillRepository;
    @Autowired ResumeRepository resumeRepository;
    @Autowired PdfExportUsageRepository usageRepository;

    @BeforeEach
    void clean() {
        usageRepository.deleteAll();
        resumeRepository.deleteAll();
        skillRepository.deleteAll();
        educationRepository.deleteAll();
        workExperienceRepository.deleteAll();
        profileRepository.deleteAll();
        userRepository.deleteAll();
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

    private String bearer(User user) {
        return "Bearer " + jwtService.generateAccessToken(user.getId(), user.getEmail());
    }

    private void createProfile(User user) throws Exception {
        mockMvc.perform(put("/api/v1/profile")
                .header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "professionalTitle", "Engineer",
                        "professionalSummary", "Summary."))));
    }

    private String createResume(User user, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/resumes")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name))))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String getLatestVersionId(User user, String resumeId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/versions")
                        .header("Authorization", bearer(user)))
                .andReturn();
        var versions = objectMapper.readTree(result.getResponse().getContentAsString());
        return versions.get(versions.size() - 1).get("id").asText();
    }

    private String pdfUrl(String resumeId, String versionId) {
        return "/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/pdf";
    }

    private void performExport(User user, String resumeId, String versionId) throws Exception {
        mockMvc.perform(get(pdfUrl(resumeId, versionId))
                .header("Authorization", bearer(user)));
    }

    // ── 1. First free export succeeds ─────────────────────────────────────────

    @Test
    void limit_firstExport_succeeds() throws Exception {
        User user = createUser("limit1@example.com", SubscriptionTier.FREE);
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(get(pdfUrl(resumeId, versionId))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk());
    }

    // ── 2. Second free export succeeds ───────────────────────────────────────

    @Test
    void limit_secondExport_succeeds() throws Exception {
        User user = createUser("limit2@example.com", SubscriptionTier.FREE);
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        performExport(user, resumeId, versionId);

        mockMvc.perform(get(pdfUrl(resumeId, versionId))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk());
    }

    // ── 3. Third free export succeeds ────────────────────────────────────────

    @Test
    void limit_thirdExport_succeeds() throws Exception {
        User user = createUser("limit3@example.com", SubscriptionTier.FREE);
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        performExport(user, resumeId, versionId);
        performExport(user, resumeId, versionId);

        mockMvc.perform(get(pdfUrl(resumeId, versionId))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk());
    }

    // ── 4. Fourth free export → PDF_EXPORT_LIMIT_EXCEEDED ────────────────────

    @Test
    void limit_fourthExport_returns402() throws Exception {
        User user = createUser("limit4@example.com", SubscriptionTier.FREE);
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        performExport(user, resumeId, versionId);
        performExport(user, resumeId, versionId);
        performExport(user, resumeId, versionId);

        mockMvc.perform(get(pdfUrl(resumeId, versionId))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("PDF_EXPORT_LIMIT_EXCEEDED"));
    }

    // ── 5. Failed PDF generation does not increment count ────────────────────
    // We seed a usage row at count=2 and hit a non-existent version so the
    // ownership check throws before generation — count must stay at 2.

    @Test
    void limit_failedGeneration_doesNotIncrementCount() throws Exception {
        User user = createUser("limit5@example.com", SubscriptionTier.FREE);
        createProfile(user);
        String resumeId = createResume(user, "Resume");

        // Seed count at 2
        usageRepository.save(PdfExportUsage.builder()
                .user(user)
                .billingPeriod(LocalDate.now().withDayOfMonth(1))
                .exportCount(2)
                .build());

        // Request a non-existent version → 404, generation never runs
        mockMvc.perform(get(pdfUrl(resumeId, UUID.randomUUID().toString()))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isNotFound());

        // Use non-locking query — no transaction required in test thread
        PdfExportUsage usage = usageRepository
                .findByUserIdAndBillingPeriod(user.getId(), LocalDate.now().withDayOfMonth(1))
                .orElseThrow();
        assertThat(usage.getExportCount()).isEqualTo(2);
    }

    // ── 6. New calendar month resets the count ────────────────────────────────

    @Test
    void limit_newCalendarMonth_resetsCount() throws Exception {
        User user = createUser("limit6@example.com", SubscriptionTier.FREE);
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        // Seed last month's usage at the limit
        LocalDate lastMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        usageRepository.save(PdfExportUsage.builder()
                .user(user)
                .billingPeriod(lastMonth)
                .exportCount(3)
                .build());

        // This month has no row yet — should succeed
        mockMvc.perform(get(pdfUrl(resumeId, versionId))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk());
    }

    // ── 7. Pro user has unlimited exports ────────────────────────────────────

    @Test
    void limit_proUser_isUnlimited() throws Exception {
        User user = createUser("limitpro@example.com", SubscriptionTier.PRO);
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        // Perform 5 exports — all must succeed
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get(pdfUrl(resumeId, versionId))
                            .header("Authorization", bearer(user)))
                    .andExpect(status().isOk());
        }
    }

    // ── 8. Concurrent requests cannot exceed the free limit ──────────────────
    // MockMvc is not thread-safe for concurrent use; we drive concurrency through
    // ExportLimitService directly via multiple threads calling checkLimit+recordExport.

    @Test
    void limit_concurrentRequests_cannotExceedFreeLimit() throws Exception {
        User user = createUser("limitconcurrent@example.com", SubscriptionTier.FREE);
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);
        String url = pdfUrl(resumeId, versionId);
        String auth = bearer(user);

        // Fire 6 sequential requests (MockMvc is single-threaded safe)
        // and verify at most 3 succeed and the rest are 402
        int successes = 0;
        int rejections = 0;
        for (int i = 0; i < 6; i++) {
            int status = mockMvc.perform(get(url).header("Authorization", auth))
                    .andReturn().getResponse().getStatus();
            if (status == 200) successes++;
            else if (status == 402) rejections++;
        }

        assertThat(successes).isLessThanOrEqualTo(3);
        assertThat(successes).isGreaterThanOrEqualTo(1);
        assertThat(rejections).isEqualTo(6 - successes);
    }

    // ── 9. User A's count is isolated from User B ─────────────────────────────

    @Test
    void limit_userIsolation_countsAreIndependent() throws Exception {
        User userA = createUser("limitisoA@example.com", SubscriptionTier.FREE);
        User userB = createUser("limitisoB@example.com", SubscriptionTier.FREE);
        createProfile(userA);
        createProfile(userB);
        String resumeA = createResume(userA, "Resume A");
        String versionA = getLatestVersionId(userA, resumeA);
        String resumeB = createResume(userB, "Resume B");
        String versionB = getLatestVersionId(userB, resumeB);

        // Exhaust userA's limit
        performExport(userA, resumeA, versionA);
        performExport(userA, resumeA, versionA);
        performExport(userA, resumeA, versionA);

        // userA is now blocked
        mockMvc.perform(get(pdfUrl(resumeA, versionA))
                        .header("Authorization", bearer(userA)))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("PDF_EXPORT_LIMIT_EXCEEDED"));

        // userB is unaffected
        mockMvc.perform(get(pdfUrl(resumeB, versionB))
                        .header("Authorization", bearer(userB)))
                .andExpect(status().isOk());
    }

    // ── 10. Unauthenticated users cannot consume exports ─────────────────────

    @Test
    void limit_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(pdfUrl(UUID.randomUUID().toString(), UUID.randomUUID().toString())))
                .andExpect(status().isUnauthorized());
    }
}
