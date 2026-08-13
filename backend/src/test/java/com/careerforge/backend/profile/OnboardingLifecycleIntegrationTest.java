package com.careerforge.backend.profile;

import com.careerforge.backend.AbstractIntegrationTest;
import com.careerforge.backend.auth.repository.PasswordResetTokenRepository;
import com.careerforge.backend.auth.repository.UserRepository;
import com.careerforge.backend.profile.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class OnboardingLifecycleIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordResetTokenRepository resetTokenRepository;
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JsonNode register(String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Password1"
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private String login(String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Password1"
                        ))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String post201Id(String token, String url, Map<String, Object> body) throws Exception {
        String response = mockMvc.perform(post(url)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    // ── Test 1: Full new-user onboarding lifecycle ────────────────────────────

    @Test
    void newUserOnboarding_fullLifecycle() throws Exception {

        // Step 1 & 2: Register — must succeed with 201
        JsonNode regResponse = register("lifecycle@example.com");
        assertThat(regResponse.get("accessToken").asText()).isNotBlank();
        assertThat(regResponse.get("userId").asText()).isNotBlank();

        // Step 3: Exactly one MasterProfile exists for this user
        UUID userId = UUID.fromString(regResponse.get("userId").asText());
        assertThat(profileRepository.existsByUserId(userId)).isTrue();
        assertThat(profileRepository.findAll()).hasSize(1);
        UUID profileId = profileRepository.findByUserId(userId).orElseThrow().getId();

        // Step 4: Login — must succeed and return a fresh token
        String token = login("lifecycle@example.com");
        assertThat(token).isNotBlank();

        // Step 5–8: GET all profile sub-resources → 200 / empty lists
        mockMvc.perform(get("/api/v1/profile").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty());

        mockMvc.perform(get("/api/v1/profile/education").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/v1/profile/skills").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/v1/profile/experience").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Step 9–11: Create one of each sub-resource
        String eduId = post201Id(token, "/api/v1/profile/education", Map.of(
                "institutionName", "MIT",
                "degree", "B.Sc. Computer Science",
                "startDate", "2016-09-01",
                "endDate", "2020-06-01",
                "displayOrder", 0
        ));

        String skillId = post201Id(token, "/api/v1/profile/skills", Map.of(
                "name", "Java",
                "proficiency", "ADVANCED",
                "displayOrder", 0
        ));

        String expId = post201Id(token, "/api/v1/profile/experience", Map.of(
                "companyName", "Acme Corp",
                "jobTitle", "Software Engineer",
                "startDate", "2020-01-01",
                "currentlyWorking", true,
                "displayOrder", 0
        ));

        // Step 12: GET all three again — each list has exactly one item
        mockMvc.perform(get("/api/v1/profile/education").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(eduId))
                .andExpect(jsonPath("$[0].institutionName").value("MIT"));

        mockMvc.perform(get("/api/v1/profile/skills").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(skillId))
                .andExpect(jsonPath("$[0].name").value("Java"));

        mockMvc.perform(get("/api/v1/profile/experience").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(expId))
                .andExpect(jsonPath("$[0].companyName").value("Acme Corp"));

        // Step 13: Verify DB-level ownership — all sub-resources belong to user 1's profile
        assertThat(educationRepository.findById(UUID.fromString(eduId)))
                .isPresent()
                .hasValueSatisfying(e -> assertThat(e.getProfile().getId()).isEqualTo(profileId));

        assertThat(skillRepository.findById(UUID.fromString(skillId)))
                .isPresent()
                .hasValueSatisfying(s -> assertThat(s.getProfile().getId()).isEqualTo(profileId));

        assertThat(workExperienceRepository.findById(UUID.fromString(expId)))
                .isPresent()
                .hasValueSatisfying(w -> assertThat(w.getProfile().getId()).isEqualTo(profileId));
    }

    // ── Test 2: Cross-user isolation ──────────────────────────────────────────

    @Test
    void crossUserIsolation_user2CannotAccessOrModifyUser1Data() throws Exception {

        // Register user 1 and create data
        JsonNode reg1 = register("user1@lifecycle.com");
        String token1 = reg1.get("accessToken").asText();

        String eduId   = post201Id(token1, "/api/v1/profile/education", Map.of(
                "institutionName", "Harvard", "displayOrder", 0));
        String skillId = post201Id(token1, "/api/v1/profile/skills", Map.of(
                "name", "Python", "displayOrder", 0));
        String expId   = post201Id(token1, "/api/v1/profile/experience", Map.of(
                "companyName", "Corp A", "jobTitle", "Dev",
                "startDate", "2019-01-01", "currentlyWorking", true, "displayOrder", 0));

        // Register user 2 and login
        register("user2@lifecycle.com");
        String token2 = login("user2@lifecycle.com");

        // User 2's own lists are empty
        mockMvc.perform(get("/api/v1/profile/education").header("Authorization", bearer(token2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/v1/profile/skills").header("Authorization", bearer(token2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/v1/profile/experience").header("Authorization", bearer(token2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // User 2 cannot delete user 1's education
        mockMvc.perform(delete("/api/v1/profile/education/" + eduId)
                        .header("Authorization", bearer(token2)))
                .andExpect(status().isNotFound());

        // User 2 cannot delete user 1's skill
        mockMvc.perform(delete("/api/v1/profile/skills/" + skillId)
                        .header("Authorization", bearer(token2)))
                .andExpect(status().isNotFound());

        // User 2 cannot delete user 1's experience
        mockMvc.perform(delete("/api/v1/profile/experience/" + expId)
                        .header("Authorization", bearer(token2)))
                .andExpect(status().isNotFound());

        // User 2 cannot update user 1's education
        mockMvc.perform(put("/api/v1/profile/education/" + eduId)
                        .header("Authorization", bearer(token2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "institutionName", "Stolen University", "displayOrder", 0))))
                .andExpect(status().isNotFound());

        // User 2 cannot update user 1's skill
        mockMvc.perform(put("/api/v1/profile/skills/" + skillId)
                        .header("Authorization", bearer(token2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Stolen Skill", "displayOrder", 0))))
                .andExpect(status().isNotFound());

        // User 2 cannot update user 1's experience
        mockMvc.perform(put("/api/v1/profile/experience/" + expId)
                        .header("Authorization", bearer(token2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "companyName", "Stolen Corp", "jobTitle", "Dev",
                                "startDate", "2019-01-01", "currentlyWorking", true, "displayOrder", 0))))
                .andExpect(status().isNotFound());

        // User 1's data is untouched — all three items still exist
        mockMvc.perform(get("/api/v1/profile/education").header("Authorization", bearer(token1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/v1/profile/skills").header("Authorization", bearer(token1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/v1/profile/experience").header("Authorization", bearer(token1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}
