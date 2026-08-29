package com.careerforge.backend.demo;

import com.careerforge.backend.AbstractIntegrationTest;
import com.careerforge.backend.application.repository.ApplicationRepository;
import com.careerforge.backend.auth.repository.UserRepository;
import com.careerforge.backend.billing.SubscriptionRepository;
import com.careerforge.backend.pdf.repository.PdfExportUsageRepository;
import com.careerforge.backend.profile.repository.MasterProfileRepository;
import com.careerforge.backend.resume.repository.ResumeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.demo.mode=true",
        "app.demo.user-email=ctrl-test@careerforge.dev",
        "app.demo.user-password=CtrlTest1!",
        "app.env="
})
class DemoControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired MasterProfileRepository profileRepository;
    @Autowired ResumeRepository resumeRepository;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired PdfExportUsageRepository pdfExportUsageRepository;
    @Autowired DemoSeedService demoSeedService;

    private static final String DEMO_EMAIL = "ctrl-test@careerforge.dev";

    @BeforeEach
    void resetDemoUser() {
        // Clean up the other-user created in the isolation test
        userRepository.findByEmail("other-ctrl@example.com").ifPresent(user -> {
            subscriptionRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                    .forEach(subscriptionRepository::delete);
            profileRepository.findByUserId(user.getId()).ifPresent(profileRepository::delete);
            userRepository.delete(user);
        });
        // Delete and re-seed so each test starts with a clean, known demo user state.
        userRepository.findByEmail(DEMO_EMAIL).ifPresent(user -> {
            applicationRepository.findByUserIdOrderByApplicationDateDesc(user.getId())
                    .forEach(applicationRepository::delete);
            pdfExportUsageRepository.findTop5ByUserIdOrderByUpdatedAtDesc(user.getId())
                    .forEach(pdfExportUsageRepository::delete);
            subscriptionRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                    .forEach(subscriptionRepository::delete);
            resumeRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                    .forEach(resumeRepository::delete);
            profileRepository.findByUserId(user.getId()).ifPresent(profileRepository::delete);
            userRepository.delete(user);
        });
        demoSeedService.seed();
    }

    @Test
    void demoLogin_demoModeOn_returns200WithTokens() throws Exception {
        mockMvc.perform(post("/api/v1/demo/login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.email").value(DEMO_EMAIL));
    }

    @Test
    void demoLogin_returnsTokenThatAuthenticatesDashboard() throws Exception {
        String response = mockMvc.perform(post("/api/v1/demo/login"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String token = mapper.readTree(response).get("accessToken").asText();

        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void demoLogin_demoUserCannotAccessOtherUserData() throws Exception {
        // Register a separate user
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"other-ctrl@example.com\",\"password\":\"Password1\",\"firstName\":\"Other\",\"lastName\":\"User\"}"))
                .andExpect(status().isCreated());

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        // Get other user's resumes (empty list — not demo user's)
        String otherLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"other-ctrl@example.com\",\"password\":\"Password1\"}"))
                .andReturn().getResponse().getContentAsString();
        String otherToken = mapper.readTree(otherLogin).get("accessToken").asText();

        // Demo user token
        String demoResponse = mockMvc.perform(post("/api/v1/demo/login"))
                .andReturn().getResponse().getContentAsString();
        String demoToken = mapper.readTree(demoResponse).get("accessToken").asText();

        // Demo user's resumes should not include other user's resumes
        String demoResumes = mockMvc.perform(get("/api/v1/resumes")
                        .header("Authorization", "Bearer " + demoToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String otherResumes = mockMvc.perform(get("/api/v1/resumes")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Other user has 0 resumes; demo user has 2 — they are isolated
        com.fasterxml.jackson.databind.JsonNode demoList = mapper.readTree(demoResumes);
        com.fasterxml.jackson.databind.JsonNode otherList = mapper.readTree(otherResumes);
        org.assertj.core.api.Assertions.assertThat(demoList.size()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(otherList.size()).isEqualTo(0);
    }

    @Test
    void demoLogin_endpointIsPublic_noAuthHeaderRequired() throws Exception {
        // Verify the endpoint is accessible without a JWT (permit-all in SecurityConfig)
        mockMvc.perform(post("/api/v1/demo/login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }
}
