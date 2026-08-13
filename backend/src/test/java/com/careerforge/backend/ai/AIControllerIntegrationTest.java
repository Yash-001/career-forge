package com.careerforge.backend.ai;

import com.careerforge.backend.AbstractIntegrationTest;
import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.auth.repository.UserRepository;
import com.careerforge.backend.profile.repository.MasterProfileRepository;
import com.careerforge.backend.profile.repository.WorkExperienceRepository;
import com.careerforge.backend.profile.repository.EducationRepository;
import com.careerforge.backend.profile.repository.SkillRepository;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AIControllerIntegrationTest extends AbstractIntegrationTest {

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

    @BeforeEach
    void clean() {
        resumeRepository.deleteAll();
        skillRepository.deleteAll();
        educationRepository.deleteAll();
        workExperienceRepository.deleteAll();
        profileRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User createUser(String email) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Password1"))
                .subscriptionTier(SubscriptionTier.FREE)
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
                        "professionalTitle", "Software Engineer",
                        "professionalSummary", "Experienced engineer"
                ))));
    }

    private void addSkillToProfile(User user, String name) throws Exception {
        mockMvc.perform(post("/api/v1/profile/skills")
                .header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "name", name, "displayOrder", 0
                ))));
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

    private String analyzeUrl(String resumeId, String versionId) {
        return "/api/v1/ai/resumes/" + resumeId + "/versions/" + versionId + "/analyze";
    }

    private String tailorUrl(String resumeId, String versionId) {
        return "/api/v1/ai/resumes/" + resumeId + "/versions/" + versionId + "/tailor";
    }

    private String acceptUrl(String resumeId, String versionId) {
        return "/api/v1/ai/resumes/" + resumeId + "/versions/" + versionId + "/accept-tailoring";
    }

    /** Tailors the version and returns the first suggestion's experienceId. */
    private String getFirstExperienceIdFromTailor(User user, String resumeId, String versionId) throws Exception {
        MvcResult result = mockMvc.perform(post(tailorUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobDescription", "Java, Spring Boot, REST APIs, Docker."
                        ))))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("suggestions").get(0).get("experienceId").asText();
    }

    private void addExperienceToProfile(User user, String description) throws Exception {
        mockMvc.perform(post("/api/v1/profile/experience")
                .header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "companyName", "Acme Corp",
                        "jobTitle", "Engineer",
                        "startDate", "2020-01-01",
                        "currentlyWorking", true,
                        "description", description,
                        "displayOrder", 0
                ))));
    }

    // ── 1. Authenticated valid analysis ───────────────────────────────────────

    @Test
    void analyze_authenticatedValidRequest_returns200() throws Exception {
        User user = createUser("analyze@example.com");
        createProfile(user);
        String resumeId = createResume(user, "My Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(post(analyzeUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobDescription", "Looking for a backend engineer with Java and Spring Boot."
                        ))))
                .andExpect(status().isOk());
    }

    // ── 2. Unauthenticated → 401 ──────────────────────────────────────────────

    @Test
    void analyze_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/ai/resumes/" + UUID.randomUUID()
                        + "/versions/" + UUID.randomUUID() + "/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobDescription", "Java Spring Boot"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    // ── 3. Blank job description → 400 ───────────────────────────────────────

    @Test
    void analyze_blankJobDescription_returns400() throws Exception {
        User user = createUser("blankjd@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(post(analyzeUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("jobDescription", "   "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ── 4. Missing required field → 400 ──────────────────────────────────────

    @Test
    void analyze_missingJobDescription_returns400() throws Exception {
        User user = createUser("missingjd@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(post(analyzeUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ── 5. Non-existent resume → 404 ─────────────────────────────────────────

    @Test
    void analyze_nonExistentResume_returns404() throws Exception {
        User user = createUser("noresume@example.com");

        mockMvc.perform(post("/api/v1/ai/resumes/" + UUID.randomUUID()
                        + "/versions/" + UUID.randomUUID() + "/analyze")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobDescription", "Java Spring Boot"
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESUME_NOT_FOUND"));
    }

    // ── 6. Non-existent version → 404 ────────────────────────────────────────

    @Test
    void analyze_nonExistentVersion_returns404() throws Exception {
        User user = createUser("noversion@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");

        mockMvc.perform(post("/api/v1/ai/resumes/" + resumeId
                        + "/versions/" + UUID.randomUUID() + "/analyze")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobDescription", "Java Spring Boot"
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESUME_VERSION_NOT_FOUND"));
    }

    // ── 7. Cross-user resume access → 404 ────────────────────────────────────

    @Test
    void analyze_crossUserResume_returns404() throws Exception {
        User userA = createUser("aiOwnerA@example.com");
        User userB = createUser("aiOwnerB@example.com");
        createProfile(userA);
        String resumeId = createResume(userA, "A's Resume");
        String versionId = getLatestVersionId(userA, resumeId);

        mockMvc.perform(post(analyzeUrl(resumeId, versionId))
                        .header("Authorization", bearer(userB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobDescription", "Java Spring Boot"
                        ))))
                .andExpect(status().isNotFound());
    }

    // ── 8. Response envelope fields ───────────────────────────────────────────

    @Test
    void analyze_responseContainsExpectedFields() throws Exception {
        User user = createUser("fields@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(post(analyzeUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobDescription", "Backend engineer with Java Spring Boot AWS Docker REST APIs."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detectedRole").isNotEmpty())
                .andExpect(jsonPath("$.keywords").isArray())
                .andExpect(jsonPath("$.technologies").isArray())
                .andExpect(jsonPath("$.responsibilities").isArray())
                .andExpect(jsonPath("$.matchedResumeSkills").isArray())
                .andExpect(jsonPath("$.missingSkills").isArray())
                .andExpect(jsonPath("$.providerName").isNotEmpty());
    }

    // ── 9. Analysis data — technology extraction ──────────────────────────────

    @Test
    void analyze_extractsTechnologiesFromJobDescription() throws Exception {
        User user = createUser("techextract@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(post(analyzeUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobDescription", "We need Java, Spring Boot, AWS, and Docker."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.technologies", hasItem("java")))
                .andExpect(jsonPath("$.technologies", hasItem("spring boot")))
                .andExpect(jsonPath("$.technologies", hasItem("aws")))
                .andExpect(jsonPath("$.technologies", hasItem("docker")));
    }

    // ── 10. Matched skills from resume ────────────────────────────────────────

    @Test
    void analyze_matchesResumeSkillsAgainstJobDescription() throws Exception {
        User user = createUser("matchskills@example.com");
        createProfile(user);
        addSkillToProfile(user, "Java");
        addSkillToProfile(user, "Docker");
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(post(analyzeUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobDescription", "Requires Java, Spring Boot, and Docker."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchedResumeSkills", hasItem("java")))
                .andExpect(jsonPath("$.matchedResumeSkills", hasItem("docker")));
    }

    // ── 11. Demo provider — no external network ───────────────────────────────

    @Test
    void analyze_demoProvider_worksWithoutExternalNetwork() throws Exception {
        User user = createUser("demoprovider@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        // This test passes only if the demo provider is active and requires no external call.
        // If an external provider were misconfigured, this would fail or time out.
        mockMvc.perform(post(analyzeUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobDescription", "Java backend engineer with Spring Boot experience."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerName", containsString("Demo")));
    }

    // ── 12. Job description size limit ───────────────────────────────────────

    @Test
    void analyze_jobDescriptionExceedsLimit_returns400() throws Exception {
        User user = createUser("toolong@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        String oversized = "a".repeat(10001);

        mockMvc.perform(post(analyzeUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("jobDescription", oversized))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // Tailoring endpoint tests
    // POST /api/v1/ai/resumes/{resumeId}/versions/{versionId}/tailor
    // ════════════════════════════════════════════════════════════════════════

    // ── T1. Successful tailoring ──────────────────────────────────────────────

    @Test
    void tailor_authenticatedValidRequest_returns200() throws Exception {
        User user = createUser("tailor1@example.com");
        createProfile(user);
        addExperienceToProfile(user, "Developed REST APIs using Spring Boot.");
        String resumeId = createResume(user, "Tailor Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(post(tailorUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobDescription", "Java, Spring Boot, REST APIs, scalable backend systems."
                        ))))
                .andExpect(status().isOk());
    }

    // ── T2. Unauthenticated → 401 ─────────────────────────────────────────────

    @Test
    void tailor_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/ai/resumes/" + UUID.randomUUID()
                        + "/versions/" + UUID.randomUUID() + "/tailor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobDescription", "Java Spring Boot"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    // ── T3. Blank job description → 400 ──────────────────────────────────────

    @Test
    void tailor_blankJobDescription_returns400() throws Exception {
        User user = createUser("tailor3@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(post(tailorUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("jobDescription", "   "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ── T4. Oversized job description → 400 ──────────────────────────────────

    @Test
    void tailor_jobDescriptionExceedsLimit_returns400() throws Exception {
        User user = createUser("tailor4@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(post(tailorUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("jobDescription", "a".repeat(10001)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ── T5. Non-existent resume → 404 ─────────────────────────────────────────

    @Test
    void tailor_nonExistentResume_returns404() throws Exception {
        User user = createUser("tailor5@example.com");

        mockMvc.perform(post("/api/v1/ai/resumes/" + UUID.randomUUID()
                        + "/versions/" + UUID.randomUUID() + "/tailor")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobDescription", "Java Spring Boot"
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESUME_NOT_FOUND"));
    }

    // ── T6. Non-existent version → 404 ───────────────────────────────────────

    @Test
    void tailor_nonExistentVersion_returns404() throws Exception {
        User user = createUser("tailor6@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");

        mockMvc.perform(post("/api/v1/ai/resumes/" + resumeId
                        + "/versions/" + UUID.randomUUID() + "/tailor")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobDescription", "Java Spring Boot"
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESUME_VERSION_NOT_FOUND"));
    }

    // ── T7. Cross-user resume → 404 ───────────────────────────────────────────

    @Test
    void tailor_crossUserResume_returns404() throws Exception {
        User userA = createUser("tailorOwnerA@example.com");
        User userB = createUser("tailorOwnerB@example.com");
        createProfile(userA);
        String resumeId = createResume(userA, "A's Resume");
        String versionId = getLatestVersionId(userA, resumeId);

        mockMvc.perform(post(tailorUrl(resumeId, versionId))
                        .header("Authorization", bearer(userB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobDescription", "Java Spring Boot"
                        ))))
                .andExpect(status().isNotFound());
    }

    // ── T8. Cross-user version (resume owned by A, version from A, accessed by B) → 404

    @Test
    void tailor_crossUserVersion_returns404() throws Exception {
        User userA = createUser("tailorVersionA@example.com");
        User userB = createUser("tailorVersionB@example.com");
        createProfile(userA);
        createProfile(userB);
        String resumeA = createResume(userA, "A Resume");
        String versionA = getLatestVersionId(userA, resumeA);
        String resumeB = createResume(userB, "B Resume");

        // userB tries to tailor userA's version using userB's resumeId
        mockMvc.perform(post(tailorUrl(resumeB, versionA))
                        .header("Authorization", bearer(userB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobDescription", "Java Spring Boot"
                        ))))
                .andExpect(status().isNotFound());
    }

    // ── T9. Response envelope is correct ─────────────────────────────────────

    @Test
    void tailor_responseContainsExpectedFields() throws Exception {
        User user = createUser("tailor9@example.com");
        createProfile(user);
        addExperienceToProfile(user, "Built microservices with Java and Docker.");
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(post(tailorUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobDescription", "Java, Docker, microservices, REST APIs."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions").isArray())
                .andExpect(jsonPath("$.detectedKeywords").isArray())
                .andExpect(jsonPath("$.providerName").isNotEmpty());
    }

    // ── T10. Response contains original bullet ────────────────────────────────

    @Test
    void tailor_responseContainsOriginalBullet() throws Exception {
        User user = createUser("tailor10@example.com");
        createProfile(user);
        addExperienceToProfile(user, "Developed REST APIs using Spring Boot.");
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(post(tailorUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobDescription", "Java, Spring Boot, REST APIs."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions[0].originalText").value("Developed REST APIs using Spring Boot."));
    }

    // ── T11. Response contains tailored suggestion ────────────────────────────

    @Test
    void tailor_responseContainsTailoredSuggestion() throws Exception {
        User user = createUser("tailor11@example.com");
        createProfile(user);
        addExperienceToProfile(user, "Developed REST APIs using Spring Boot.");
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(post(tailorUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobDescription", "Java, Spring Boot, REST APIs."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions[0].suggestedText").isNotEmpty())
                .andExpect(jsonPath("$.suggestions[0].rationale").isNotEmpty());
    }

    // ── T12. Response contains matched keywords and rationale ─────────────────

    @Test
    void tailor_responseContainsMatchedKeywordsAndRationale() throws Exception {
        User user = createUser("tailor12@example.com");
        createProfile(user);
        addExperienceToProfile(user, "Deployed services using Docker and Kubernetes.");
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(post(tailorUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobDescription", "Docker, Kubernetes, AWS, CI/CD pipelines."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions[0].matchedKeywords").isArray())
                .andExpect(jsonPath("$.suggestions[0].rationale").isNotEmpty());
    }

    // ── T13. Tailoring does not modify the stored resume ──────────────────────

    @Test
    void tailor_doesNotModifyStoredResume() throws Exception {
        User user = createUser("tailor13@example.com");
        createProfile(user);
        addExperienceToProfile(user, "Developed REST APIs using Spring Boot.");
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        // Fetch version content before tailoring
        MvcResult before = mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/versions/" + versionId)
                        .header("Authorization", bearer(user)))
                .andReturn();
        String contentBefore = before.getResponse().getContentAsString();

        // Tailor
        mockMvc.perform(post(tailorUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobDescription", "Java, Spring Boot, REST APIs, scalable backend."
                        ))))
                .andExpect(status().isOk());

        // Fetch version content after tailoring — must be identical
        MvcResult after = mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/versions/" + versionId)
                        .header("Authorization", bearer(user)))
                .andReturn();
        String contentAfter = after.getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertEquals(contentBefore, contentAfter,
                "Resume version content must not change after tailoring");
    }

    // ── T14. Tailoring does not modify MasterProfile ──────────────────────────

    @Test
    void tailor_doesNotModifyMasterProfile() throws Exception {
        User user = createUser("tailor14@example.com");
        createProfile(user);
        addExperienceToProfile(user, "Developed REST APIs using Spring Boot.");
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        // Fetch profile before tailoring
        MvcResult before = mockMvc.perform(get("/api/v1/profile")
                        .header("Authorization", bearer(user)))
                .andReturn();
        String profileBefore = before.getResponse().getContentAsString();

        // Tailor
        mockMvc.perform(post(tailorUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobDescription", "Java, Spring Boot, REST APIs."
                        ))))
                .andExpect(status().isOk());

        // Fetch profile after tailoring — must be identical
        MvcResult after = mockMvc.perform(get("/api/v1/profile")
                        .header("Authorization", bearer(user)))
                .andReturn();
        String profileAfter = after.getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertEquals(profileBefore, profileAfter,
                "MasterProfile must not change after tailoring");
    }

    // ════════════════════════════════════════════════════════════════════════
    // Accept-tailoring endpoint tests
    // POST /api/v1/ai/resumes/{resumeId}/versions/{versionId}/accept-tailoring
    // ════════════════════════════════════════════════════════════════════════

    // ── A1. Accept one suggestion → 201 with new version ─────────────────────

    @Test
    void accept_oneSuggestion_returns201WithNewVersion() throws Exception {
        User user = createUser("accept1@example.com");
        createProfile(user);
        addExperienceToProfile(user, "Developed REST APIs using Spring Boot.");
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);
        String expId = getFirstExperienceIdFromTailor(user, resumeId, versionId);

        mockMvc.perform(post(acceptUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "acceptedSuggestions", List.of(Map.of(
                                        "experienceId", expId,
                                        "suggestedText", "Engineered scalable REST APIs with Spring Boot."
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.versionNumber").value(2));
    }

    // ── A2. Unauthenticated → 401 ─────────────────────────────────────────────

    @Test
    void accept_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/ai/resumes/" + UUID.randomUUID()
                        + "/versions/" + UUID.randomUUID() + "/accept-tailoring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "acceptedSuggestions", List.of(Map.of(
                                        "experienceId", UUID.randomUUID().toString(),
                                        "suggestedText", "Some text."
                                ))
                        ))))
                .andExpect(status().isUnauthorized());
    }

    // ── A3. Empty acceptedSuggestions list → 400 ──────────────────────────────

    @Test
    void accept_emptyList_returns400() throws Exception {
        User user = createUser("accept3@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(post(acceptUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "acceptedSuggestions", List.of()
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ── A4. Non-existent resume → 404 ─────────────────────────────────────────

    @Test
    void accept_nonExistentResume_returns404() throws Exception {
        User user = createUser("accept4@example.com");

        mockMvc.perform(post("/api/v1/ai/resumes/" + UUID.randomUUID()
                        + "/versions/" + UUID.randomUUID() + "/accept-tailoring")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "acceptedSuggestions", List.of(Map.of(
                                        "experienceId", UUID.randomUUID().toString(),
                                        "suggestedText", "Some text."
                                ))
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESUME_NOT_FOUND"));
    }

    // ── A5. Non-existent version → 404 ───────────────────────────────────────

    @Test
    void accept_nonExistentVersion_returns404() throws Exception {
        User user = createUser("accept5@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");

        mockMvc.perform(post("/api/v1/ai/resumes/" + resumeId
                        + "/versions/" + UUID.randomUUID() + "/accept-tailoring")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "acceptedSuggestions", List.of(Map.of(
                                        "experienceId", UUID.randomUUID().toString(),
                                        "suggestedText", "Some text."
                                ))
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESUME_VERSION_NOT_FOUND"));
    }

    // ── A6. Cross-user → 404 ──────────────────────────────────────────────────

    @Test
    void accept_crossUser_returns404() throws Exception {
        User userA = createUser("acceptOwnerA@example.com");
        User userB = createUser("acceptOwnerB@example.com");
        createProfile(userA);
        String resumeId = createResume(userA, "A's Resume");
        String versionId = getLatestVersionId(userA, resumeId);

        mockMvc.perform(post(acceptUrl(resumeId, versionId))
                        .header("Authorization", bearer(userB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "acceptedSuggestions", List.of(Map.of(
                                        "experienceId", UUID.randomUUID().toString(),
                                        "suggestedText", "Some text."
                                ))
                        ))))
                .andExpect(status().isNotFound());
    }

    // ── A7. Invalid experienceId (random UUID) → 422 ──────────────────────────

    @Test
    void accept_invalidExperienceId_returns422() throws Exception {
        User user = createUser("accept7@example.com");
        createProfile(user);
        addExperienceToProfile(user, "Developed REST APIs.");
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(post(acceptUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "acceptedSuggestions", List.of(Map.of(
                                        "experienceId", UUID.randomUUID().toString(),
                                        "suggestedText", "Some text."
                                ))
                        ))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_SUGGESTION"));
    }

    // ── A8. experienceId from a different version → 422 ──────────────────────

    @Test
    void accept_experienceIdFromDifferentVersion_returns422() throws Exception {
        User user = createUser("accept8@example.com");
        createProfile(user);
        addExperienceToProfile(user, "Developed REST APIs.");
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        // Create a second version — its experience IDs differ from version 1
        mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/versions")
                .header("Authorization", bearer(user)));
        String version2Id = getLatestVersionId(user, resumeId);
        String expIdFromV2 = getFirstExperienceIdFromTailor(user, resumeId, version2Id);

        // Try to accept using version 1 but with an experienceId from version 2
        mockMvc.perform(post(acceptUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "acceptedSuggestions", List.of(Map.of(
                                        "experienceId", expIdFromV2,
                                        "suggestedText", "Some text."
                                ))
                        ))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_SUGGESTION"));
    }

    // ── A9. Source version is unchanged after accept ───────────────────────────

    @Test
    void accept_sourceVersionUnchanged() throws Exception {
        User user = createUser("accept9@example.com");
        createProfile(user);
        addExperienceToProfile(user, "Developed REST APIs using Spring Boot.");
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);
        String expId = getFirstExperienceIdFromTailor(user, resumeId, versionId);

        mockMvc.perform(post(acceptUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "acceptedSuggestions", List.of(Map.of(
                                        "experienceId", expId,
                                        "suggestedText", "Engineered scalable REST APIs."
                                ))
                        ))))
                .andExpect(status().isCreated());

        // Source version experience description must be unchanged
        mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/versions/" + versionId)
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.experiences[0].description").value("Developed REST APIs using Spring Boot."))
                .andExpect(jsonPath("$.versionNumber").value(1));
    }

    // ── A10. New version has the accepted bullet text ─────────────────────────

    @Test
    void accept_newVersionContainsAcceptedBullet() throws Exception {
        User user = createUser("accept10@example.com");
        createProfile(user);
        addExperienceToProfile(user, "Developed REST APIs using Spring Boot.");
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);
        String expId = getFirstExperienceIdFromTailor(user, resumeId, versionId);
        String tailoredText = "Engineered scalable REST APIs with Spring Boot and Docker.";

        MvcResult result = mockMvc.perform(post(acceptUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "acceptedSuggestions", List.of(Map.of(
                                        "experienceId", expId,
                                        "suggestedText", tailoredText
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        mockMvc.perform(post(acceptUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "acceptedSuggestions", List.of(Map.of(
                                        "experienceId", expId,
                                        "suggestedText", tailoredText
                                ))
                        ))))
                .andExpect(status().isCreated());

        String newVersionId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/versions/" + newVersionId)
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.experiences[0].description").value(tailoredText));
    }

    // ── A11. New version title gets " — AI Tailored" suffix ───────────────────

    @Test
    void accept_newVersionTitleHasAiTailoredSuffix() throws Exception {
        User user = createUser("accept11@example.com");
        createProfile(user);
        addExperienceToProfile(user, "Developed REST APIs.");
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);
        String expId = getFirstExperienceIdFromTailor(user, resumeId, versionId);

        mockMvc.perform(post(acceptUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "acceptedSuggestions", List.of(Map.of(
                                        "experienceId", expId,
                                        "suggestedText", "Engineered scalable REST APIs."
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", containsString("AI Tailored")));
    }

    // ── A12. No double suffix when source already ends with " — AI Tailored" ──

    @Test
    void accept_noDoubleSuffix_whenSourceAlreadyTailored() throws Exception {
        User user = createUser("accept12@example.com");
        createProfile(user);
        addExperienceToProfile(user, "Developed REST APIs.");
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);
        String expId = getFirstExperienceIdFromTailor(user, resumeId, versionId);

        // First acceptance — creates " — AI Tailored" version
        MvcResult first = mockMvc.perform(post(acceptUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "acceptedSuggestions", List.of(Map.of(
                                        "experienceId", expId,
                                        "suggestedText", "Engineered scalable REST APIs."
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        String tailoredVersionId = objectMapper.readTree(first.getResponse().getContentAsString()).get("id").asText();
        String expIdV2 = getFirstExperienceIdFromTailor(user, resumeId, tailoredVersionId);

        // Second acceptance from the already-tailored version
        mockMvc.perform(post(acceptUrl(resumeId, tailoredVersionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "acceptedSuggestions", List.of(Map.of(
                                        "experienceId", expIdV2,
                                        "suggestedText", "Delivered scalable REST APIs."
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", not(containsString("AI Tailored — AI Tailored"))));
    }

    // ── A13. Version number increments correctly ──────────────────────────────

    @Test
    void accept_versionNumberIncrements() throws Exception {
        User user = createUser("accept13@example.com");
        createProfile(user);
        addExperienceToProfile(user, "Developed REST APIs.");
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);
        String expId = getFirstExperienceIdFromTailor(user, resumeId, versionId);

        mockMvc.perform(post(acceptUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "acceptedSuggestions", List.of(Map.of(
                                        "experienceId", expId,
                                        "suggestedText", "Engineered scalable REST APIs."
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionNumber").value(2));
    }

    // ── A14. Educations and skills are copied verbatim ────────────────────────

    @Test
    void accept_educationsAndSkillsCopiedVerbatim() throws Exception {
        User user = createUser("accept14@example.com");
        createProfile(user);
        addExperienceToProfile(user, "Developed REST APIs.");
        addSkillToProfile(user, "Java");
        mockMvc.perform(post("/api/v1/profile/education")
                .header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "institutionName", "MIT",
                        "degree", "BSc",
                        "startDate", "2015-09-01",
                        "displayOrder", 0
                ))));
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);
        String expId = getFirstExperienceIdFromTailor(user, resumeId, versionId);

        mockMvc.perform(post(acceptUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "acceptedSuggestions", List.of(Map.of(
                                        "experienceId", expId,
                                        "suggestedText", "Engineered scalable REST APIs."
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.educations[0].institutionName").value("MIT"))
                .andExpect(jsonPath("$.skills[0].name").value("Java"));
    }

    // ── A15. Repeated acceptance creates separate new versions ────────────────

    @Test
    void accept_repeatedAcceptance_createsSeparateVersions() throws Exception {
        User user = createUser("accept15@example.com");
        createProfile(user);
        addExperienceToProfile(user, "Developed REST APIs.");
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);
        String expId = getFirstExperienceIdFromTailor(user, resumeId, versionId);

        String body = objectMapper.writeValueAsString(Map.of(
                "acceptedSuggestions", List.of(Map.of(
                        "experienceId", expId,
                        "suggestedText", "Engineered scalable REST APIs."
                ))
        ));

        MvcResult r1 = mockMvc.perform(post(acceptUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();

        MvcResult r2 = mockMvc.perform(post(acceptUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();

        String id1 = objectMapper.readTree(r1.getResponse().getContentAsString()).get("id").asText();
        String id2 = objectMapper.readTree(r2.getResponse().getContentAsString()).get("id").asText();
        org.junit.jupiter.api.Assertions.assertNotEquals(id1, id2, "Each acceptance must produce a distinct version");
    }

    // ── A16. Tailor response includes experienceId on each suggestion ─────────

    @Test
    void tailor_responseIncludesExperienceIdOnSuggestions() throws Exception {
        User user = createUser("accept16@example.com");
        createProfile(user);
        addExperienceToProfile(user, "Developed REST APIs using Spring Boot.");
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(post(tailorUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobDescription", "Java, Spring Boot, REST APIs."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions[0].experienceId").isNotEmpty());
    }

    // ── T15. Repeated identical request produces deterministic output ─────────

    @Test
    void tailor_repeatedIdenticalRequest_isDeterministic() throws Exception {
        User user = createUser("tailor15@example.com");
        createProfile(user);
        addExperienceToProfile(user, "Developed REST APIs using Spring Boot.");
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        String body = objectMapper.writeValueAsString(Map.of(
                "jobDescription", "Java, Spring Boot, REST APIs, Docker, AWS."
        ));

        MvcResult first = mockMvc.perform(post(tailorUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult second = mockMvc.perform(post(tailorUrl(resumeId, versionId))
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        org.junit.jupiter.api.Assertions.assertEquals(
                first.getResponse().getContentAsString(),
                second.getResponse().getContentAsString(),
                "Tailoring output must be deterministic for identical inputs"
        );
    }
}
