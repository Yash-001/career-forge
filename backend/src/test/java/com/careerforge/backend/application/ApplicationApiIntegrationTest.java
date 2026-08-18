package com.careerforge.backend.application;

import com.careerforge.backend.AbstractIntegrationTest;
import com.careerforge.backend.application.repository.ApplicationRepository;
import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.auth.repository.UserRepository;
import com.careerforge.backend.profile.repository.*;
import com.careerforge.backend.resume.repository.ResumeRepository;
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
class ApplicationApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired ResumeRepository resumeRepository;
    @Autowired MasterProfileRepository profileRepository;
    @Autowired WorkExperienceRepository workExperienceRepository;
    @Autowired EducationRepository educationRepository;
    @Autowired SkillRepository skillRepository;

    @BeforeEach
    void clean() {
        applicationRepository.deleteAll();
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
                        "professionalSummary", "Summary"
                ))));
    }

    private String createResume(User user) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/resumes")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "My Resume"))))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String getLatestVersionId(User user, String resumeId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/versions")
                        .header("Authorization", bearer(user)))
                .andReturn();
        JsonNode versions = objectMapper.readTree(result.getResponse().getContentAsString());
        return versions.get(versions.size() - 1).get("id").asText();
    }

    private String createApplication(User user, String company, String role) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", company,
                                "jobTitle", role,
                                "applicationDate", "2024-06-01"
                        ))))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Test
    void create_success_returns201() throws Exception {
        User user = createUser("create@example.com");

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", "Acme Corp",
                                "jobTitle", "Backend Engineer",
                                "applicationDate", "2024-06-15",
                                "jobUrl", "https://acme.com/jobs/1",
                                "status", "APPLIED"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.companyName").value("Acme Corp"))
                .andExpect(jsonPath("$.jobTitle").value("Backend Engineer"))
                .andExpect(jsonPath("$.applicationDate").value("2024-06-15"))
                .andExpect(jsonPath("$.jobUrl").value("https://acme.com/jobs/1"))
                .andExpect(jsonPath("$.status").value("APPLIED"));
    }

    @Test
    void create_defaultsStatusToApplied_whenStatusOmitted() throws Exception {
        User user = createUser("defaultstatus@example.com");

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", "Corp",
                                "jobTitle", "Dev",
                                "applicationDate", "2024-06-01"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPLIED"));
    }

    @Test
    void create_withResumeVersion_linksCorrectly() throws Exception {
        User user = createUser("withversion@example.com");
        createProfile(user);
        String resumeId = createResume(user);
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", "Tech Co",
                                "jobTitle", "Dev",
                                "applicationDate", "2024-07-01",
                                "resumeVersionId", versionId
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resumeVersionId").value(versionId));
    }

    @Test
    void create_crossUserResumeVersion_returns404() throws Exception {
        User userA = createUser("versionOwnerA@example.com");
        User userB = createUser("versionOwnerB@example.com");
        createProfile(userA);
        String resumeId = createResume(userA);
        String versionId = getLatestVersionId(userA, resumeId);

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", bearer(userB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", "Corp",
                                "jobTitle", "Dev",
                                "applicationDate", "2024-07-01",
                                "resumeVersionId", versionId
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESUME_VERSION_NOT_FOUND"));
    }

    // ── List ──────────────────────────────────────────────────────────────────

    @Test
    void list_returns200WithOwnedApplications() throws Exception {
        User user = createUser("list@example.com");
        createApplication(user, "Corp A", "Dev");
        createApplication(user, "Corp B", "Lead");

        mockMvc.perform(get("/api/v1/applications").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void list_containsOnlyAuthenticatedUsersApplications() throws Exception {
        User userA = createUser("listA@example.com");
        User userB = createUser("listB@example.com");
        createApplication(userA, "Corp A", "Dev");
        createApplication(userB, "Corp B", "Lead");

        mockMvc.perform(get("/api/v1/applications").header("Authorization", bearer(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].companyName").value("Corp A"));
    }

    @Test
    void list_emptyWhenNoApplications() throws Exception {
        User user = createUser("emptylist@example.com");

        mockMvc.perform(get("/api/v1/applications").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ── Get ───────────────────────────────────────────────────────────────────

    @Test
    void get_returns200WithCorrectData() throws Exception {
        User user = createUser("get@example.com");
        String appId = createApplication(user, "Acme", "Engineer");

        mockMvc.perform(get("/api/v1/applications/" + appId).header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(appId))
                .andExpect(jsonPath("$.companyName").value("Acme"));
    }

    @Test
    void get_nonExistent_returns404() throws Exception {
        User user = createUser("getne@example.com");

        mockMvc.perform(get("/api/v1/applications/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("APPLICATION_NOT_FOUND"));
    }

    @Test
    void get_crossUser_returns404() throws Exception {
        User userA = createUser("getxA@example.com");
        User userB = createUser("getxB@example.com");
        String appId = createApplication(userA, "Corp", "Dev");

        mockMvc.perform(get("/api/v1/applications/" + appId).header("Authorization", bearer(userB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("APPLICATION_NOT_FOUND"));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Test
    void update_returns200WithUpdatedData() throws Exception {
        User user = createUser("update@example.com");
        String appId = createApplication(user, "Old Corp", "Junior Dev");

        mockMvc.perform(put("/api/v1/applications/" + appId)
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", "New Corp",
                                "jobTitle", "Senior Dev",
                                "applicationDate", "2024-08-01",
                                "status", "INTERVIEW"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("New Corp"))
                .andExpect(jsonPath("$.jobTitle").value("Senior Dev"))
                .andExpect(jsonPath("$.status").value("INTERVIEW"));
    }

    @Test
    void update_nonExistent_returns404() throws Exception {
        User user = createUser("updne@example.com");

        mockMvc.perform(put("/api/v1/applications/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", "Corp",
                                "jobTitle", "Dev",
                                "applicationDate", "2024-06-01",
                                "status", "APPLIED"
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("APPLICATION_NOT_FOUND"));
    }

    @Test
    void update_crossUser_returns404() throws Exception {
        User userA = createUser("updxA@example.com");
        User userB = createUser("updxB@example.com");
        String appId = createApplication(userA, "Corp", "Dev");

        mockMvc.perform(put("/api/v1/applications/" + appId)
                        .header("Authorization", bearer(userB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", "Hijacked",
                                "jobTitle", "Dev",
                                "applicationDate", "2024-06-01",
                                "status", "APPLIED"
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("APPLICATION_NOT_FOUND"));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_returns204() throws Exception {
        User user = createUser("delete@example.com");
        String appId = createApplication(user, "Corp", "Dev");

        mockMvc.perform(delete("/api/v1/applications/" + appId).header("Authorization", bearer(user)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/applications/" + appId).header("Authorization", bearer(user)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_nonExistent_returns404() throws Exception {
        User user = createUser("delnf@example.com");

        mockMvc.perform(delete("/api/v1/applications/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("APPLICATION_NOT_FOUND"));
    }

    @Test
    void delete_crossUser_returns404() throws Exception {
        User userA = createUser("delxA@example.com");
        User userB = createUser("delxB@example.com");
        String appId = createApplication(userA, "Corp", "Dev");

        mockMvc.perform(delete("/api/v1/applications/" + appId).header("Authorization", bearer(userB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("APPLICATION_NOT_FOUND"));
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    @Test
    void unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/applications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticated_post_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", "Corp", "jobTitle", "Dev", "applicationDate", "2024-06-01"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    void create_missingCompanyName_returns400() throws Exception {
        User user = createUser("valcomp@example.com");

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobTitle", "Dev", "applicationDate", "2024-06-01"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_missingJobTitle_returns400() throws Exception {
        User user = createUser("valtitle@example.com");

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", "Corp", "applicationDate", "2024-06-01"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_missingApplicationDate_returns400() throws Exception {
        User user = createUser("valdate@example.com");

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", "Corp", "jobTitle", "Dev"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_invalidUrl_returns400() throws Exception {
        User user = createUser("valurl@example.com");

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", "Corp",
                                "jobTitle", "Dev",
                                "applicationDate", "2024-06-01",
                                "jobUrl", "not-a-url"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_invalidStatus_returns400() throws Exception {
        User user = createUser("valstatus@example.com");

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Corp\",\"jobTitle\":\"Dev\",\"applicationDate\":\"2024-06-01\",\"status\":\"INVALID_STATUS\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_missingRequiredFields_returns400() throws Exception {
        User user = createUser("updval@example.com");
        String appId = createApplication(user, "Corp", "Dev");

        mockMvc.perform(put("/api/v1/applications/" + appId)
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobTitle", "Dev", "applicationDate", "2024-06-01", "status", "APPLIED"
                        ))))
                .andExpect(status().isBadRequest());
    }
}
