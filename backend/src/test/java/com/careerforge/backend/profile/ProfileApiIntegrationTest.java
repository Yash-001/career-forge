package com.careerforge.backend.profile;

import com.careerforge.backend.AbstractIntegrationTest;
import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.auth.repository.PasswordResetTokenRepository;
import com.careerforge.backend.auth.repository.UserRepository;
import com.careerforge.backend.profile.repository.*;
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

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ProfileApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordResetTokenRepository resetTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    @Autowired MasterProfileRepository profileRepository;
    @Autowired WorkExperienceRepository workExperienceRepository;
    @Autowired EducationRepository educationRepository;
    @Autowired SkillRepository skillRepository;

    @BeforeEach
    void clean() {
        skillRepository.deleteAll();
        educationRepository.deleteAll();
        workExperienceRepository.deleteAll();
        profileRepository.deleteAll();
        resetTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ── Auth helpers ──────────────────────────────────────────────────────────

    /**
     * Registers a user via the API — this is the real onboarding path.
     * Registration now creates the MasterProfile automatically.
     * Returns the access token.
     */
    private String registerAndGetToken(String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Password1"
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    /**
     * Creates a User directly (bypassing registration) — used only for tests
     * that explicitly verify the PROFILE_NOT_FOUND path.
     */
    private User createUserWithoutProfile(String email) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Password1"))
                .subscriptionTier(SubscriptionTier.FREE)
                .enabled(true)
                .build());
    }

    private String tokenFor(User user) {
        return jwtService.generateAccessToken(user.getId(), user.getEmail());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String bearerFor(User user) {
        return "Bearer " + tokenFor(user);
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    @Test
    void getProfile_afterRegistration_returns200() throws Exception {
        String token = registerAndGetToken("getprofile@example.com");

        mockMvc.perform(get("/api/v1/profile").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void getProfile_noProfile_returns404() throws Exception {
        User user = createUserWithoutProfile("noProfile@example.com");
        mockMvc.perform(get("/api/v1/profile").header("Authorization", bearerFor(user)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_FOUND"));
    }

    @Test
    void upsertProfile_updatesExistingProfile_returns200() throws Exception {
        String token = registerAndGetToken("update@example.com");

        mockMvc.perform(put("/api/v1/profile")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("professionalTitle", "Senior Dev"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.professionalTitle").value("Senior Dev"));
    }

    @Test
    void upsertProfile_createsProfile_returns200() throws Exception {
        // User created without profile (legacy path) — upsert still creates one
        User user = createUserWithoutProfile("create@example.com");

        mockMvc.perform(put("/api/v1/profile")
                        .header("Authorization", bearerFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "professionalTitle", "Software Engineer",
                                "location", "New York"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.professionalTitle").value("Software Engineer"))
                .andExpect(jsonPath("$.location").value("New York"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void getProfile_afterUpsert_returns200() throws Exception {
        String token = registerAndGetToken("get@example.com");

        mockMvc.perform(put("/api/v1/profile")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("phone", "555-1234"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/profile").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("555-1234"));
    }

    @Test
    void profile_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void upsertProfile_invalidUrl_returns400() throws Exception {
        String token = registerAndGetToken("badurl@example.com");

        mockMvc.perform(put("/api/v1/profile")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("linkedinUrl", "not-a-url"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ── Work Experience ───────────────────────────────────────────────────────

    @Test
    void getExperiences_afterRegistration_returnsEmptyList() throws Exception {
        String token = registerAndGetToken("expEmpty@example.com");

        mockMvc.perform(get("/api/v1/profile/experience").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void createExperience_noProfile_returns404() throws Exception {
        User user = createUserWithoutProfile("expNoProfile@example.com");
        mockMvc.perform(post("/api/v1/profile/experience")
                        .header("Authorization", bearerFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", "Acme", "jobTitle", "Dev",
                                "startDate", "2020-01-01", "currentlyWorking", true
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_FOUND"));
    }

    @Test
    void createExperience_success_returns201() throws Exception {
        String token = registerAndGetToken("expCreate@example.com");

        mockMvc.perform(post("/api/v1/profile/experience")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", "Acme Corp",
                                "jobTitle", "Software Engineer",
                                "startDate", "2020-01-01",
                                "currentlyWorking", true,
                                "displayOrder", 0
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.companyName").value("Acme Corp"))
                .andExpect(jsonPath("$.currentlyWorking").value(true));
    }

    @Test
    void getExperiences_returnsOwnedList() throws Exception {
        String token = registerAndGetToken("expList@example.com");
        createExperienceFor(token, "Company A", 1);
        createExperienceFor(token, "Company B", 2);

        mockMvc.perform(get("/api/v1/profile/experience").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].companyName").value("Company A"))
                .andExpect(jsonPath("$[1].companyName").value("Company B"));
    }

    @Test
    void updateExperience_success_returns200() throws Exception {
        String token = registerAndGetToken("expUpdate@example.com");
        String id = createExperienceFor(token, "Old Corp", 0);

        mockMvc.perform(put("/api/v1/profile/experience/" + id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", "New Corp",
                                "jobTitle", "Lead Engineer",
                                "startDate", "2021-06-01",
                                "currentlyWorking", true,
                                "displayOrder", 0
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("New Corp"));
    }

    @Test
    void deleteExperience_success_returns204() throws Exception {
        String token = registerAndGetToken("expDelete@example.com");
        String id = createExperienceFor(token, "Corp", 0);

        mockMvc.perform(delete("/api/v1/profile/experience/" + id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/profile/experience").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void createExperience_currentlyWorkingWithEndDate_returns400() throws Exception {
        String token = registerAndGetToken("expBadDates@example.com");

        mockMvc.perform(post("/api/v1/profile/experience")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", "Corp", "jobTitle", "Dev",
                                "startDate", "2020-01-01", "endDate", "2022-01-01",
                                "currentlyWorking", true, "displayOrder", 0
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createExperience_missingRequiredField_returns400() throws Exception {
        String token = registerAndGetToken("expMissing@example.com");

        mockMvc.perform(post("/api/v1/profile/experience")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "jobTitle", "Dev", "startDate", "2020-01-01"
                                // missing companyName
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void experience_crossUserOwnership_returns404() throws Exception {
        String tokenA = registerAndGetToken("expOwnerA@example.com");
        String tokenB = registerAndGetToken("expOwnerB@example.com");
        String idBelongingToA = createExperienceFor(tokenA, "A Corp", 0);

        // User B sees their own empty list
        mockMvc.perform(get("/api/v1/profile/experience").header("Authorization", bearer(tokenB)))
                .andExpect(jsonPath("$", hasSize(0)));

        // User B cannot delete User A's experience
        mockMvc.perform(delete("/api/v1/profile/experience/" + idBelongingToA)
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());
    }

    // ── Education ─────────────────────────────────────────────────────────────

    @Test
    void getEducations_afterRegistration_returnsEmptyList() throws Exception {
        String token = registerAndGetToken("eduEmpty@example.com");

        mockMvc.perform(get("/api/v1/profile/education").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void createEducation_success_returns201() throws Exception {
        String token = registerAndGetToken("eduCreate@example.com");

        mockMvc.perform(post("/api/v1/profile/education")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "institutionName", "MIT",
                                "degree", "B.Sc. Computer Science",
                                "startDate", "2016-09-01",
                                "endDate", "2020-06-01",
                                "displayOrder", 0
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.institutionName").value("MIT"));
    }

    @Test
    void getEducations_returnsOwnedList() throws Exception {
        String token = registerAndGetToken("eduList@example.com");
        createEducationFor(token, "MIT", 1);
        createEducationFor(token, "Harvard", 2);

        mockMvc.perform(get("/api/v1/profile/education").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void updateEducation_success_returns200() throws Exception {
        String token = registerAndGetToken("eduUpdate@example.com");
        String id = createEducationFor(token, "Old University", 0);

        mockMvc.perform(put("/api/v1/profile/education/" + id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "institutionName", "New University",
                                "displayOrder", 0
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.institutionName").value("New University"));
    }

    @Test
    void deleteEducation_success_returns204() throws Exception {
        String token = registerAndGetToken("eduDelete@example.com");
        String id = createEducationFor(token, "MIT", 0);

        mockMvc.perform(delete("/api/v1/profile/education/" + id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
    }

    @Test
    void createEducation_endDateBeforeStartDate_returns400() throws Exception {
        String token = registerAndGetToken("eduBadDates@example.com");

        mockMvc.perform(post("/api/v1/profile/education")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "institutionName", "MIT",
                                "startDate", "2022-01-01",
                                "endDate", "2020-01-01",
                                "displayOrder", 0
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEducation_missingInstitutionName_returns400() throws Exception {
        String token = registerAndGetToken("eduMissing@example.com");

        mockMvc.perform(post("/api/v1/profile/education")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("degree", "B.Sc."))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void education_crossUserOwnership_returns404() throws Exception {
        String tokenA = registerAndGetToken("eduOwnerA@example.com");
        String tokenB = registerAndGetToken("eduOwnerB@example.com");
        String idBelongingToA = createEducationFor(tokenA, "MIT", 0);

        mockMvc.perform(delete("/api/v1/profile/education/" + idBelongingToA)
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());
    }

    // ── Skills ────────────────────────────────────────────────────────────────

    @Test
    void getSkills_afterRegistration_returnsEmptyList() throws Exception {
        String token = registerAndGetToken("skillEmpty@example.com");

        mockMvc.perform(get("/api/v1/profile/skills").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void createSkill_success_returns201() throws Exception {
        String token = registerAndGetToken("skillCreate@example.com");

        mockMvc.perform(post("/api/v1/profile/skills")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Java",
                                "proficiency", "ADVANCED",
                                "displayOrder", 0
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Java"))
                .andExpect(jsonPath("$.proficiency").value("ADVANCED"));
    }

    @Test
    void getSkills_returnsOwnedList() throws Exception {
        String token = registerAndGetToken("skillList@example.com");
        createSkillFor(token, "Java", 1);
        createSkillFor(token, "Python", 2);

        mockMvc.perform(get("/api/v1/profile/skills").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Java"))
                .andExpect(jsonPath("$[1].name").value("Python"));
    }

    @Test
    void updateSkill_success_returns200() throws Exception {
        String token = registerAndGetToken("skillUpdate@example.com");
        String id = createSkillFor(token, "Java", 0);

        mockMvc.perform(put("/api/v1/profile/skills/" + id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Kotlin",
                                "displayOrder", 0
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kotlin"));
    }

    @Test
    void deleteSkill_success_returns204() throws Exception {
        String token = registerAndGetToken("skillDelete@example.com");
        String id = createSkillFor(token, "Java", 0);

        mockMvc.perform(delete("/api/v1/profile/skills/" + id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
    }

    @Test
    void createSkill_blankName_returns400() throws Exception {
        String token = registerAndGetToken("skillBlank@example.com");

        mockMvc.perform(post("/api/v1/profile/skills")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "  ", "displayOrder", 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void skill_crossUserOwnership_returns404() throws Exception {
        String tokenA = registerAndGetToken("skillOwnerA@example.com");
        String tokenB = registerAndGetToken("skillOwnerB@example.com");
        String idBelongingToA = createSkillFor(tokenA, "Java", 0);

        mockMvc.perform(delete("/api/v1/profile/skills/" + idBelongingToA)
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());
    }

    @Test
    void skills_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/profile/skills"))
                .andExpect(status().isUnauthorized());
    }

    // ── Test helpers ──────────────────────────────────────────────────────────

    private String createExperienceFor(String token, String company, int order) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/profile/experience")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", company,
                                "jobTitle", "Engineer",
                                "startDate", "2020-01-01",
                                "currentlyWorking", true,
                                "displayOrder", order
                        ))))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createEducationFor(String token, String institution, int order) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/profile/education")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "institutionName", institution,
                                "displayOrder", order
                        ))))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createSkillFor(String token, String name, int order) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/profile/skills")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name,
                                "displayOrder", order
                        ))))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
