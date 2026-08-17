package com.careerforge.backend.pdf;

import com.careerforge.backend.AbstractIntegrationTest;
import com.careerforge.backend.auth.domain.SubscriptionTier;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.auth.repository.UserRepository;
import com.careerforge.backend.profile.repository.EducationRepository;
import com.careerforge.backend.profile.repository.MasterProfileRepository;
import com.careerforge.backend.profile.repository.SkillRepository;
import com.careerforge.backend.profile.repository.WorkExperienceRepository;
import com.careerforge.backend.resume.repository.ResumeRepository;
import com.careerforge.backend.shared.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.PdfReader;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class PdfControllerIntegrationTest extends AbstractIntegrationTest {

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
                        "professionalSummary", "Experienced backend engineer."
                ))));
    }

    private void addExperience(User user) throws Exception {
        mockMvc.perform(post("/api/v1/profile/experience")
                .header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "companyName", "Acme Corp",
                        "jobTitle", "Backend Engineer",
                        "startDate", "2021-01-01",
                        "currentlyWorking", true,
                        "description", "Built REST APIs with Spring Boot.",
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
        var versions = objectMapper.readTree(result.getResponse().getContentAsString());
        return versions.get(versions.size() - 1).get("id").asText();
    }

    private String pdfUrl(String resumeId, String versionId) {
        return "/api/v1/resumes/" + resumeId + "/versions/" + versionId + "/pdf";
    }

    // ── 1. Authenticated owner → 200 with PDF bytes ───────────────────────────

    @Test
    void pdf_authenticatedOwner_returns200() throws Exception {
        User user = createUser("pdfowner@example.com");
        createProfile(user);
        String resumeId = createResume(user, "My Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(get(pdfUrl(resumeId, versionId))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk());
    }

    // ── 2. Unauthenticated → 401 ──────────────────────────────────────────────

    @Test
    void pdf_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(pdfUrl(UUID.randomUUID().toString(), UUID.randomUUID().toString())))
                .andExpect(status().isUnauthorized());
    }

    // ── 3. Non-existent resume → 404 RESUME_NOT_FOUND ────────────────────────

    @Test
    void pdf_nonExistentResume_returns404() throws Exception {
        User user = createUser("pdfnoresume@example.com");

        mockMvc.perform(get(pdfUrl(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESUME_NOT_FOUND"));
    }

    // ── 4. Non-existent version → 404 RESUME_VERSION_NOT_FOUND ───────────────

    @Test
    void pdf_nonExistentVersion_returns404() throws Exception {
        User user = createUser("pdfnoversion@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");

        mockMvc.perform(get(pdfUrl(resumeId, UUID.randomUUID().toString()))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESUME_VERSION_NOT_FOUND"));
    }

    // ── 5. Cross-user resume → 404, no existence leakage ─────────────────────

    @Test
    void pdf_crossUserResume_returns404() throws Exception {
        User userA = createUser("pdfOwnerA@example.com");
        User userB = createUser("pdfOwnerB@example.com");
        createProfile(userA);
        String resumeId = createResume(userA, "A's Resume");
        String versionId = getLatestVersionId(userA, resumeId);

        mockMvc.perform(get(pdfUrl(resumeId, versionId))
                        .header("Authorization", bearer(userB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESUME_NOT_FOUND"));
    }

    // ── 6. Cross-user version (B's resumeId + A's versionId) → 404 ───────────

    @Test
    void pdf_crossUserVersion_returns404() throws Exception {
        User userA = createUser("pdfVersionA@example.com");
        User userB = createUser("pdfVersionB@example.com");
        createProfile(userA);
        createProfile(userB);
        String resumeA = createResume(userA, "A Resume");
        String versionA = getLatestVersionId(userA, resumeA);
        String resumeB = createResume(userB, "B Resume");

        // userB uses their own resumeId but userA's versionId
        mockMvc.perform(get(pdfUrl(resumeB, versionA))
                        .header("Authorization", bearer(userB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESUME_VERSION_NOT_FOUND"));
    }

    // ── 7. Content-Type is application/pdf ───────────────────────────────────

    @Test
    void pdf_responseContentType_isApplicationPdf() throws Exception {
        User user = createUser("pdfcontenttype@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(get(pdfUrl(resumeId, versionId))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    // ── 8. Content-Disposition is attachment with .pdf filename ──────────────

    @Test
    void pdf_contentDisposition_isAttachmentWithPdfFilename() throws Exception {
        User user = createUser("pdfdisposition@example.com");
        createProfile(user);
        String resumeId = createResume(user, "My Resume");
        String versionId = getLatestVersionId(user, resumeId);

        mockMvc.perform(get(pdfUrl(resumeId, versionId))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString(".pdf")));
    }

    // ── 9. Response body is a valid PDF (magic bytes %PDF) ────────────────────

    @Test
    void pdf_responseBody_isValidPdf() throws Exception {
        User user = createUser("pdfvalid@example.com");
        createProfile(user);
        String resumeId = createResume(user, "Resume");
        String versionId = getLatestVersionId(user, resumeId);

        MvcResult result = mockMvc.perform(get(pdfUrl(resumeId, versionId))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andReturn();

        byte[] pdf = result.getResponse().getContentAsByteArray();
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");

        // Verify OpenPDF can parse it without error
        PdfReader reader = new PdfReader(pdf);
        assertThat(reader.getNumberOfPages()).isGreaterThan(0);
        reader.close();
    }

    // ── 10. PDF contains content from the correct ResumeVersion ──────────────

    @Test
    void pdf_containsContentFromCorrectResumeVersion() throws Exception {
        User user = createUser("pdfcontent@example.com");
        createProfile(user);
        addExperience(user);
        String resumeId = createResume(user, "Content Resume");
        String versionId = getLatestVersionId(user, resumeId);

        MvcResult result = mockMvc.perform(get(pdfUrl(resumeId, versionId))
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andReturn();

        byte[] pdf = result.getResponse().getContentAsByteArray();
        PdfReader reader = new PdfReader(pdf);
        com.lowagie.text.pdf.parser.PdfTextExtractor extractor =
                new com.lowagie.text.pdf.parser.PdfTextExtractor(reader);

        StringBuilder text = new StringBuilder();
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            text.append(extractor.getTextFromPage(i));
        }
        reader.close();

        // Summary from profile snapshot
        assertThat(text.toString()).contains("Experienced backend engineer");
        // Experience from profile snapshot
        assertThat(text.toString()).contains("Acme Corp");
        assertThat(text.toString()).contains("Backend Engineer");
    }
}
