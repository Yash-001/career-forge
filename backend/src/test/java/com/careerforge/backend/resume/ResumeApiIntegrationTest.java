package com.careerforge.backend.resume;

import com.careerforge.backend.AbstractIntegrationTest;
import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.auth.repository.UserRepository;
import com.careerforge.backend.profile.repository.*;
import com.careerforge.backend.resume.repository.*;
import com.careerforge.backend.shared.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
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

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ResumeApiIntegrationTest extends AbstractIntegrationTest {

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
    @Autowired ResumeVersionRepository resumeVersionRepository;

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
                        "professionalTitle", "Engineer",
                        "professionalSummary", "Summary text"
                ))));
    }

    private void addExperienceToProfile(User user, String company) throws Exception {
        mockMvc.perform(post("/api/v1/profile/experience")
                .header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "companyName", company,
                        "jobTitle", "Engineer",
                        "startDate", "2020-01-01",
                        "currentlyWorking", true,
                        "displayOrder", 0
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
        JsonNode versions = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode last = versions.get(versions.size() - 1);
        return last.get("id").asText();
    }

    // ── Resume CRUD ───────────────────────────────────────────────────────────

    @Test
    void createResume_success_returns201() throws Exception {
        User user = createUser("create@example.com");
        createProfile(user);

        mockMvc.perform(post("/api/v1/resumes")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "My Resume"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("My Resume"))
                .andExpect(jsonPath("$.latestVersion.versionNumber").value(1));
    }

    @Test
    void createResume_noProfile_returns404() throws Exception {
        User user = createUser("noprofile@example.com");

        mockMvc.perform(post("/api/v1/resumes")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Resume"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_FOUND"));
    }

    @Test
    void createResume_blankName_returns400() throws Exception {
        User user = createUser("blank@example.com");
        createProfile(user);

        mockMvc.perform(post("/api/v1/resumes")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "  "))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listResumes_returnsOwnedResumes() throws Exception {
        User user = createUser("list@example.com");
        createProfile(user);
        createResume(user, "Resume A");
        createResume(user, "Resume B");

        mockMvc.perform(get("/api/v1/resumes").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getResume_success_returns200() throws Exception {
        User user = createUser("get@example.com");
        createProfile(user);
        String resumeId = createResume(user, "My Resume");

        mockMvc.perform(get("/api/v1/resumes/" + resumeId).header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(resumeId))
                .andExpect(jsonPath("$.name").value("My Resume"));
    }

    @Test
    void updateResume_renamesResume() throws Exception {
        User user = createUser("rename@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Old Name");

        mockMvc.perform(put("/api/v1/resumes/" + resumeId)
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "New Name"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));
    }

    @Test
    void deleteResume_returns204() throws Exception {
        User user = createUser("delete@example.com");
        createProfile(user);
        String resumeId = createResume(user, "To Delete");

        mockMvc.perform(delete("/api/v1/resumes/" + resumeId).header("Authorization", bearer(user)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/resumes/" + resumeId).header("Authorization", bearer(user)))
                .andExpect(status().isNotFound());
    }

    @Test
    void resumes_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/resumes"))
                .andExpect(status().isUnauthorized());
    }

    // ── Ownership ─────────────────────────────────────────────────────────────

    @Test
    void getResume_crossUser_returns404() throws Exception {
        User userA = createUser("ownerA@example.com");
        User userB = createUser("ownerB@example.com");
        createProfile(userA);
        String resumeId = createResume(userA, "A's Resume");

        mockMvc.perform(get("/api/v1/resumes/" + resumeId).header("Authorization", bearer(userB)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteResume_crossUser_returns404() throws Exception {
        User userA = createUser("delA@example.com");
        User userB = createUser("delB@example.com");
        createProfile(userA);
        String resumeId = createResume(userA, "A's Resume");

        mockMvc.perform(delete("/api/v1/resumes/" + resumeId).header("Authorization", bearer(userB)))
                .andExpect(status().isNotFound());
    }

    // ── Versions ──────────────────────────────────────────────────────────────

    @Test
    void listVersions_initialVersionExists() throws Exception {
        User user = createUser("versions@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");

        mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/versions")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].versionNumber").value(1))
                .andExpect(jsonPath("$[0].isLatest").value(true));
    }

    @Test
    void createVersion_incrementsVersionNumber() throws Exception {
        User user = createUser("newversion@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");

        mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/versions")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionNumber").value(2));

        mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/versions")
                        .header("Authorization", bearer(user)))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getVersion_returnsFullSnapshot() throws Exception {
        User user = createUser("getversion@example.com");
        createProfile(user);
        addExperienceToProfile(user, "Acme Corp");
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/versions/" + versionId)
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.experiences", hasSize(1)))
                .andExpect(jsonPath("$.experiences[0].companyName").value("Acme Corp"));
    }

    @Test
    void getVersion_crossUser_returns404() throws Exception {
        User userA = createUser("versionOwnerA@example.com");
        User userB = createUser("versionOwnerB@example.com");
        createProfile(userA);
        String resumeId = createResume(userA, "A's Resume");
        String versionId = getLatestVersionId(userA, resumeId);

        mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/versions/" + versionId)
                        .header("Authorization", bearer(userB)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateVersionMeta_updatesTitle() throws Exception {
        User user = createUser("versionmeta@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(put("/api/v1/resumes/" + resumeId + "/versions/" + versionId)
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Senior Engineer",
                                "professionalSummary", "Updated summary"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Senior Engineer"))
                .andExpect(jsonPath("$.professionalSummary").value("Updated summary"));
    }

    // ── Snapshot isolation ────────────────────────────────────────────────────

    @Test
    void snapshotIsolation_profileChangeDoesNotAffectExistingVersion() throws Exception {
        User user = createUser("snapshot@example.com");
        createProfile(user);
        addExperienceToProfile(user, "Company A");
        String resumeId = createResume(user, "Resume");
        String v1Id = getLatestVersionId(user, resumeId);

        // Modify profile — add new experience
        addExperienceToProfile(user, "Company B");

        // Create v2 from updated profile
        mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/versions")
                .header("Authorization", bearer(user)));

        // V1 must still have only Company A
        mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/versions/" + v1Id)
                        .header("Authorization", bearer(user)))
                .andExpect(jsonPath("$.experiences", hasSize(1)))
                .andExpect(jsonPath("$.experiences[0].companyName").value("Company A"));

        // V2 must have both
        String v2Id = getLatestVersionId(user, resumeId);
        mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/versions/" + v2Id)
                        .header("Authorization", bearer(user)))
                .andExpect(jsonPath("$.experiences", hasSize(2)));
    }

    // ── Experience CRUD ───────────────────────────────────────────────────────

    @Test
    void addExperience_returns201() throws Exception {
        User user = createUser("addexp@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/experiences")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", "New Corp",
                                "jobTitle", "Dev",
                                "startDate", "2022-01-01",
                                "currentlyWorking", true,
                                "displayOrder", 0
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.companyName").value("New Corp"));
    }

    @Test
    void updateExperience_success() throws Exception {
        User user = createUser("updateexp@example.com");
        createProfile(user);
        addExperienceToProfile(user, "Old Corp");
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        MvcResult versionResult = mockMvc.perform(
                        get("/api/v1/resumes/" + resumeId + "/versions/" + versionId)
                                .header("Authorization", bearer(user)))
                .andReturn();
        String expId = objectMapper.readTree(versionResult.getResponse().getContentAsString())
                .get("experiences").get(0).get("id").asText();

        mockMvc.perform(put("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/experiences/" + expId)
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", "New Corp",
                                "jobTitle", "Lead",
                                "startDate", "2020-01-01",
                                "currentlyWorking", true,
                                "displayOrder", 0
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("New Corp"));
    }

    @Test
    void updateExperience_doesNotModifyProfile() throws Exception {
        User user = createUser("expiso@example.com");
        createProfile(user);
        addExperienceToProfile(user, "ABC Ltd.");
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        MvcResult versionResult = mockMvc.perform(
                        get("/api/v1/resumes/" + resumeId + "/versions/" + versionId)
                                .header("Authorization", bearer(user)))
                .andReturn();
        String expId = objectMapper.readTree(versionResult.getResponse().getContentAsString())
                .get("experiences").get(0).get("id").asText();

        mockMvc.perform(put("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/experiences/" + expId)
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", "ABC Corporation",
                                "jobTitle", "Engineer",
                                "startDate", "2020-01-01",
                                "currentlyWorking", true,
                                "displayOrder", 0
                        ))))
                .andExpect(status().isOk());

        // Profile experience must remain unchanged
        mockMvc.perform(get("/api/v1/profile/experience").header("Authorization", bearer(user)))
                .andExpect(jsonPath("$[0].companyName").value("ABC Ltd."));
    }

    @Test
    void deleteExperience_returns204() throws Exception {
        User user = createUser("delexp@example.com");
        createProfile(user);
        addExperienceToProfile(user, "Corp");
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        MvcResult versionResult = mockMvc.perform(
                        get("/api/v1/resumes/" + resumeId + "/versions/" + versionId)
                                .header("Authorization", bearer(user)))
                .andReturn();
        String expId = objectMapper.readTree(versionResult.getResponse().getContentAsString())
                .get("experiences").get(0).get("id").asText();

        mockMvc.perform(delete("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/experiences/" + expId)
                        .header("Authorization", bearer(user)))
                .andExpect(status().isNoContent());
    }

    @Test
    void addExperience_invalidDates_returns400() throws Exception {
        User user = createUser("expbaddates@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/experiences")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", "Corp", "jobTitle", "Dev",
                                "startDate", "2020-01-01", "endDate", "2022-01-01",
                                "currentlyWorking", true, "displayOrder", 0
                        ))))
                .andExpect(status().isBadRequest());
    }

    // ── Education CRUD ────────────────────────────────────────────────────────

    @Test
    void addEducation_returns201() throws Exception {
        User user = createUser("addedu@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/education")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "institutionName", "MIT",
                                "degree", "B.Sc.",
                                "displayOrder", 0
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.institutionName").value("MIT"));
    }

    @Test
    void updateEducation_success() throws Exception {
        User user = createUser("updateedu@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        MvcResult addResult = mockMvc.perform(
                        post("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/education")
                                .header("Authorization", bearer(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                        "institutionName", "Old Uni", "displayOrder", 0
                                ))))
                .andReturn();
        String eduId = objectMapper.readTree(addResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(put("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/education/" + eduId)
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "institutionName", "New Uni", "displayOrder", 0
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.institutionName").value("New Uni"));
    }

    @Test
    void deleteEducation_returns204() throws Exception {
        User user = createUser("deledu@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        MvcResult addResult = mockMvc.perform(
                        post("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/education")
                                .header("Authorization", bearer(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                        "institutionName", "MIT", "displayOrder", 0
                                ))))
                .andReturn();
        String eduId = objectMapper.readTree(addResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(delete("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/education/" + eduId)
                        .header("Authorization", bearer(user)))
                .andExpect(status().isNoContent());
    }

    // ── Skills CRUD ───────────────────────────────────────────────────────────

    @Test
    void addSkill_returns201() throws Exception {
        User user = createUser("addskill@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/skills")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Java", "displayOrder", 0
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Java"));
    }

    @Test
    void updateSkill_success() throws Exception {
        User user = createUser("updateskill@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        MvcResult addResult = mockMvc.perform(
                        post("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/skills")
                                .header("Authorization", bearer(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                        "name", "Java", "displayOrder", 0
                                ))))
                .andReturn();
        String skillId = objectMapper.readTree(addResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(put("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/skills/" + skillId)
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Kotlin", "displayOrder", 0
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kotlin"));
    }

    @Test
    void deleteSkill_returns204() throws Exception {
        User user = createUser("delskill@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        MvcResult addResult = mockMvc.perform(
                        post("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/skills")
                                .header("Authorization", bearer(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                        "name", "Java", "displayOrder", 0
                                ))))
                .andReturn();
        String skillId = objectMapper.readTree(addResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(delete("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/skills/" + skillId)
                        .header("Authorization", bearer(user)))
                .andExpect(status().isNoContent());
    }

    @Test
    void addSkill_blankName_returns400() throws Exception {
        User user = createUser("blankskill@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/skills")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "  ", "displayOrder", 0
                        ))))
                .andExpect(status().isBadRequest());
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    @Test
    void unauthenticated_postResume_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/resumes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Resume"))))
                .andExpect(status().isUnauthorized());
    }

    // ── Resume not found / validation ─────────────────────────────────────────

    @Test
    void getResume_nonExistent_returns404() throws Exception {
        User user = createUser("getne@example.com");

        mockMvc.perform(get("/api/v1/resumes/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESUME_NOT_FOUND"));
    }

    @Test
    void updateResume_blankName_returns400() throws Exception {
        User user = createUser("renamblank@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");

        mockMvc.perform(put("/api/v1/resumes/" + resumeId)
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "  "))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateResume_crossUser_returns404() throws Exception {
        User userA = createUser("renameA@example.com");
        User userB = createUser("renameB@example.com");
        createProfile(userA);
        String resumeId = createResume(userA, "A's Resume");

        mockMvc.perform(put("/api/v1/resumes/" + resumeId)
                        .header("Authorization", bearer(userB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Hijacked"))))
                .andExpect(status().isNotFound());
    }

    // ── Version not found / ownership ─────────────────────────────────────────

    @Test
    void getVersion_nonExistent_returns404() throws Exception {
        User user = createUser("getvne@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");

        mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/versions/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESUME_VERSION_NOT_FOUND"));
    }

    @Test
    void listVersions_crossUser_returns404() throws Exception {
        User userA = createUser("lvA@example.com");
        User userB = createUser("lvB@example.com");
        createProfile(userA);
        String resumeId = createResume(userA, "A's Resume");

        mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/versions")
                        .header("Authorization", bearer(userB)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createVersion_crossUser_returns404() throws Exception {
        User userA = createUser("cvA@example.com");
        User userB = createUser("cvB@example.com");
        createProfile(userA);
        String resumeId = createResume(userA, "A's Resume");

        mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/versions")
                        .header("Authorization", bearer(userB)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateVersionMeta_crossUser_returns404() throws Exception {
        User userA = createUser("vmA@example.com");
        User userB = createUser("vmB@example.com");
        createProfile(userA);
        String resumeId = createResume(userA, "A's Resume");
        String versionId = getLatestVersionId(userA, resumeId);

        mockMvc.perform(put("/api/v1/resumes/" + resumeId + "/versions/" + versionId)
                        .header("Authorization", bearer(userB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Hijacked"))))
                .andExpect(status().isNotFound());
    }

    // ── Experience ownership / not found / validation ─────────────────────────

    @Test
    void addExperience_missingRequiredField_returns400() throws Exception {
        User user = createUser("expmissing@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/experiences")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobTitle", "Dev", "startDate", "2022-01-01",
                                "currentlyWorking", true, "displayOrder", 0
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateExperience_notFound_returns404() throws Exception {
        User user = createUser("expnf@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(put("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/experiences/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", "Corp", "jobTitle", "Dev",
                                "startDate", "2022-01-01", "currentlyWorking", true, "displayOrder", 0
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESUME_EXPERIENCE_NOT_FOUND"));
    }

    @Test
    void deleteExperience_notFound_returns404() throws Exception {
        User user = createUser("expnfdel@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(delete("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/experiences/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESUME_EXPERIENCE_NOT_FOUND"));
    }

    @Test
    void addExperience_crossUser_returns404() throws Exception {
        User userA = createUser("expxA@example.com");
        User userB = createUser("expxB@example.com");
        createProfile(userA);
        String resumeId = createResume(userA, "A's Resume");
        String versionId = getLatestVersionId(userA, resumeId);

        mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/experiences")
                        .header("Authorization", bearer(userB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", "Corp", "jobTitle", "Dev",
                                "startDate", "2022-01-01", "currentlyWorking", true, "displayOrder", 0
                        ))))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateExperience_crossUser_returns404() throws Exception {
        User userA = createUser("expuxA@example.com");
        User userB = createUser("expuxB@example.com");
        createProfile(userA);
        addExperienceToProfile(userA, "Corp");
        String resumeId = createResume(userA, "A's Resume");
        String versionId = getLatestVersionId(userA, resumeId);
        MvcResult vr = mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/versions/" + versionId)
                .header("Authorization", bearer(userA))).andReturn();
        String expId = objectMapper.readTree(vr.getResponse().getContentAsString())
                .get("experiences").get(0).get("id").asText();

        mockMvc.perform(put("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/experiences/" + expId)
                        .header("Authorization", bearer(userB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", "Hijacked", "jobTitle", "Dev",
                                "startDate", "2022-01-01", "currentlyWorking", true, "displayOrder", 0
                        ))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteExperience_crossUser_returns404() throws Exception {
        User userA = createUser("expdxA@example.com");
        User userB = createUser("expdxB@example.com");
        createProfile(userA);
        addExperienceToProfile(userA, "Corp");
        String resumeId = createResume(userA, "A's Resume");
        String versionId = getLatestVersionId(userA, resumeId);
        MvcResult vr = mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/versions/" + versionId)
                .header("Authorization", bearer(userA))).andReturn();
        String expId = objectMapper.readTree(vr.getResponse().getContentAsString())
                .get("experiences").get(0).get("id").asText();

        mockMvc.perform(delete("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/experiences/" + expId)
                        .header("Authorization", bearer(userB)))
                .andExpect(status().isNotFound());
    }

    // ── Education ownership / not found / validation ──────────────────────────

    @Test
    void addEducation_missingInstitutionName_returns400() throws Exception {
        User user = createUser("edumissing@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/education")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("degree", "B.Sc.", "displayOrder", 0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addEducation_invalidDates_returns400() throws Exception {
        User user = createUser("edubaddates@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/education")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "institutionName", "MIT",
                                "startDate", "2022-01-01", "endDate", "2020-01-01",
                                "displayOrder", 0
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateEducation_notFound_returns404() throws Exception {
        User user = createUser("edunf@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(put("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/education/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("institutionName", "MIT", "displayOrder", 0))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESUME_EDUCATION_NOT_FOUND"));
    }

    @Test
    void deleteEducation_notFound_returns404() throws Exception {
        User user = createUser("edunfdel@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(delete("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/education/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESUME_EDUCATION_NOT_FOUND"));
    }

    @Test
    void addEducation_crossUser_returns404() throws Exception {
        User userA = createUser("eduxA@example.com");
        User userB = createUser("eduxB@example.com");
        createProfile(userA);
        String resumeId = createResume(userA, "A's Resume");
        String versionId = getLatestVersionId(userA, resumeId);

        mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/education")
                        .header("Authorization", bearer(userB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("institutionName", "MIT", "displayOrder", 0))))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateEducation_crossUser_returns404() throws Exception {
        User userA = createUser("eduuxA@example.com");
        User userB = createUser("eduuxB@example.com");
        createProfile(userA);
        String resumeId = createResume(userA, "A's Resume");
        String versionId = getLatestVersionId(userA, resumeId);
        MvcResult addResult = mockMvc.perform(
                        post("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/education")
                                .header("Authorization", bearer(userA))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("institutionName", "MIT", "displayOrder", 0))))
                .andReturn();
        String eduId = objectMapper.readTree(addResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(put("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/education/" + eduId)
                        .header("Authorization", bearer(userB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("institutionName", "Hijacked", "displayOrder", 0))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteEducation_crossUser_returns404() throws Exception {
        User userA = createUser("edudxA@example.com");
        User userB = createUser("edudxB@example.com");
        createProfile(userA);
        String resumeId = createResume(userA, "A's Resume");
        String versionId = getLatestVersionId(userA, resumeId);
        MvcResult addResult = mockMvc.perform(
                        post("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/education")
                                .header("Authorization", bearer(userA))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("institutionName", "MIT", "displayOrder", 0))))
                .andReturn();
        String eduId = objectMapper.readTree(addResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(delete("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/education/" + eduId)
                        .header("Authorization", bearer(userB)))
                .andExpect(status().isNotFound());
    }

    // ── Skill ownership / not found ───────────────────────────────────────────

    @Test
    void updateSkill_notFound_returns404() throws Exception {
        User user = createUser("skillnf@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(put("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/skills/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Java", "displayOrder", 0))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESUME_SKILL_NOT_FOUND"));
    }

    @Test
    void deleteSkill_notFound_returns404() throws Exception {
        User user = createUser("skillnfdel@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(delete("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/skills/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESUME_SKILL_NOT_FOUND"));
    }

    @Test
    void addSkill_crossUser_returns404() throws Exception {
        User userA = createUser("skillxA@example.com");
        User userB = createUser("skillxB@example.com");
        createProfile(userA);
        String resumeId = createResume(userA, "A's Resume");
        String versionId = getLatestVersionId(userA, resumeId);

        mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/skills")
                        .header("Authorization", bearer(userB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Java", "displayOrder", 0))))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateSkill_crossUser_returns404() throws Exception {
        User userA = createUser("skilluxA@example.com");
        User userB = createUser("skilluxB@example.com");
        createProfile(userA);
        String resumeId = createResume(userA, "A's Resume");
        String versionId = getLatestVersionId(userA, resumeId);
        MvcResult addResult = mockMvc.perform(
                        post("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/skills")
                                .header("Authorization", bearer(userA))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("name", "Java", "displayOrder", 0))))
                .andReturn();
        String skillId = objectMapper.readTree(addResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(put("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/skills/" + skillId)
                        .header("Authorization", bearer(userB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Hijacked", "displayOrder", 0))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteSkill_crossUser_returns404() throws Exception {
        User userA = createUser("skilldxA@example.com");
        User userB = createUser("skilldxB@example.com");
        createProfile(userA);
        String resumeId = createResume(userA, "A's Resume");
        String versionId = getLatestVersionId(userA, resumeId);
        MvcResult addResult = mockMvc.perform(
                        post("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/skills")
                                .header("Authorization", bearer(userA))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("name", "Java", "displayOrder", 0))))
                .andReturn();
        String skillId = objectMapper.readTree(addResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(delete("/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/skills/" + skillId)
                        .header("Authorization", bearer(userB)))
                .andExpect(status().isNotFound());
    }
}
