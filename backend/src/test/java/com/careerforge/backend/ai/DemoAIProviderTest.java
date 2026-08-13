package com.careerforge.backend.ai;

import com.careerforge.backend.ai.dto.*;
import com.careerforge.backend.ai.provider.DemoAIProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DemoAIProviderTest {

    private DemoAIProvider provider;

    @BeforeEach
    void setUp() {
        provider = new DemoAIProvider();
    }

    // ── providerName ──────────────────────────────────────────────────────────

    @Test
    void providerName_isDemo() {
        assertThat(provider.providerName()).contains("Demo");
    }

    // ── analyzeJobDescription — keyword extraction ────────────────────────────

    @Test
    void analyze_extractsTechnologies() {
        JobAnalysisRequest req = new JobAnalysisRequest(
                "We need Java, Spring Boot, AWS, Docker, and REST APIs.", List.of());
        JobAnalysisResponse res = provider.analyzeJobDescription(req);

        assertThat(res.technologies()).contains("java", "spring boot", "aws", "docker", "rest apis");
    }

    @Test
    void analyze_extractionIsCaseInsensitive() {
        JobAnalysisRequest req = new JobAnalysisRequest(
                "Experience with JAVA and SPRING BOOT required.", List.of());
        JobAnalysisResponse res = provider.analyzeJobDescription(req);

        assertThat(res.technologies()).contains("java", "spring boot");
    }

    @Test
    void analyze_deduplicatesKeywords() {
        JobAnalysisRequest req = new JobAnalysisRequest(
                "Java Java Java Spring Boot Spring Boot", List.of());
        JobAnalysisResponse res = provider.analyzeJobDescription(req);

        long javaCount = res.technologies().stream().filter("java"::equals).count();
        assertThat(javaCount).isEqualTo(1);
    }

    @Test
    void analyze_unknownTechnologiesNotExtracted() {
        JobAnalysisRequest req = new JobAnalysisRequest(
                "Experience with FooLang and BarFramework required.", List.of());
        JobAnalysisResponse res = provider.analyzeJobDescription(req);

        assertThat(res.technologies()).doesNotContain("foolang", "barframework");
    }

    // ── analyzeJobDescription — role detection ────────────────────────────────

    @Test
    void analyze_detectsBackendRole() {
        JobAnalysisRequest req = new JobAnalysisRequest(
                "Looking for a backend engineer with Java experience.", List.of());
        JobAnalysisResponse res = provider.analyzeJobDescription(req);

        assertThat(res.detectedRole()).isEqualTo("Backend Engineer");
    }

    @Test
    void analyze_detectsFrontendRole() {
        JobAnalysisRequest req = new JobAnalysisRequest(
                "Frontend developer needed for React project.", List.of());
        JobAnalysisResponse res = provider.analyzeJobDescription(req);

        assertThat(res.detectedRole()).isEqualTo("Frontend Engineer");
    }

    @Test
    void analyze_defaultsToSoftwareEngineerWhenRoleUnknown() {
        JobAnalysisRequest req = new JobAnalysisRequest(
                "We need someone who knows Java.", List.of());
        JobAnalysisResponse res = provider.analyzeJobDescription(req);

        assertThat(res.detectedRole()).isEqualTo("Software Engineer");
    }

    // ── analyzeJobDescription — matched / missing skills ─────────────────────

    @Test
    void analyze_matchedSkillsFromResume() {
        JobAnalysisRequest req = new JobAnalysisRequest(
                "Requires Java, Spring Boot, and Docker.",
                List.of("Java", "Docker", "React"));
        JobAnalysisResponse res = provider.analyzeJobDescription(req);

        assertThat(res.matchedResumeSkills()).contains("java", "docker");
    }

    @Test
    void analyze_missingSkillsNotOnResume() {
        JobAnalysisRequest req = new JobAnalysisRequest(
                "Requires Java, Spring Boot, and Docker.",
                List.of("Java"));
        JobAnalysisResponse res = provider.analyzeJobDescription(req);

        assertThat(res.missingSkills()).contains("spring boot", "docker");
        assertThat(res.missingSkills()).doesNotContain("java");
    }

    @Test
    void analyze_emptyResumeSkillsAllTechAreMissing() {
        JobAnalysisRequest req = new JobAnalysisRequest(
                "Requires Java and Docker.", List.of());
        JobAnalysisResponse res = provider.analyzeJobDescription(req);

        assertThat(res.missingSkills()).contains("java", "docker");
        assertThat(res.matchedResumeSkills()).isEmpty();
    }

    // ── analyzeJobDescription — empty input ───────────────────────────────────

    @Test
    void analyze_emptyJobDescription_returnsEmptyResponse() {
        JobAnalysisRequest req = new JobAnalysisRequest("   ", List.of());
        JobAnalysisResponse res = provider.analyzeJobDescription(req);

        assertThat(res.technologies()).isEmpty();
        assertThat(res.keywords()).isEmpty();
        assertThat(res.detectedRole()).isNull();
    }

    // ── analyzeJobDescription — determinism ───────────────────────────────────

    @Test
    void analyze_isDeterministic() {
        JobAnalysisRequest req = new JobAnalysisRequest(
                "Java Spring Boot AWS Docker REST APIs backend engineer.", List.of("Java"));

        JobAnalysisResponse r1 = provider.analyzeJobDescription(req);
        JobAnalysisResponse r2 = provider.analyzeJobDescription(req);

        assertThat(r1.technologies()).isEqualTo(r2.technologies());
        assertThat(r1.detectedRole()).isEqualTo(r2.detectedRole());
        assertThat(r1.matchedResumeSkills()).isEqualTo(r2.matchedResumeSkills());
        assertThat(r1.missingSkills()).isEqualTo(r2.missingSkills());
    }

    // ── tailorResume — bullet tailoring ───────────────────────────────────────

    @Test
    void tailor_originalBulletPreserved() {
        TailoringRequest req = new TailoringRequest(
                "Java Spring Boot REST APIs",
                List.of(new BulletWithId(UUID.randomUUID(), "Developed REST APIs using Spring Boot.")),
                List.of());
        TailoringResponse res = provider.tailorResume(req);

        assertThat(res.suggestions()).hasSize(1);
        assertThat(res.suggestions().get(0).originalText())
                .isEqualTo("Developed REST APIs using Spring Boot.");
    }

    @Test
    void tailor_suggestedTextGenerated() {
        TailoringRequest req = new TailoringRequest(
                "Java Spring Boot REST APIs",
                List.of(new BulletWithId(UUID.randomUUID(), "Developed REST APIs using Spring Boot.")),
                List.of());
        TailoringResponse res = provider.tailorResume(req);

        assertThat(res.suggestions().get(0).suggestedText()).isNotBlank();
        assertThat(res.suggestions().get(0).suggestedText())
                .isNotEqualTo(res.suggestions().get(0).originalText());
    }

    @Test
    void tailor_matchedKeywordsIncluded() {
        TailoringRequest req = new TailoringRequest(
                "Java Spring Boot REST APIs",
                List.of(new BulletWithId(UUID.randomUUID(), "Developed REST APIs using Spring Boot.")),
                List.of());
        TailoringResponse res = provider.tailorResume(req);

        assertThat(res.suggestions().get(0).matchedKeywords()).isNotEmpty();
    }

    @Test
    void tailor_rationaleIncluded() {
        TailoringRequest req = new TailoringRequest(
                "Java Spring Boot",
                List.of(new BulletWithId(UUID.randomUUID(), "Built microservices with Spring Boot.")),
                List.of());
        TailoringResponse res = provider.tailorResume(req);

        assertThat(res.suggestions().get(0).rationale()).isNotBlank();
    }

    @Test
    void tailor_noKeywordOverlap_appendsGenericPhrase() {
        TailoringRequest req = new TailoringRequest(
                "Java Spring Boot",
                List.of(new BulletWithId(UUID.randomUUID(), "Managed office supplies and coordinated meetings.")),
                List.of());
        TailoringResponse res = provider.tailorResume(req);

        BulletSuggestion s = res.suggestions().get(0);
        assertThat(s.matchedKeywords()).isEmpty();
        assertThat(s.suggestedText()).contains("Aligned with job requirements");
    }

    @Test
    void tailor_isDeterministic() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        TailoringRequest req = new TailoringRequest(
                "Java Spring Boot AWS Docker",
                List.of(new BulletWithId(id1, "Developed REST APIs."),
                        new BulletWithId(id2, "Deployed services on AWS.")),
                List.of("Java", "Docker"));

        TailoringResponse r1 = provider.tailorResume(req);
        TailoringResponse r2 = provider.tailorResume(req);

        assertThat(r1.suggestions().get(0).suggestedText())
                .isEqualTo(r2.suggestions().get(0).suggestedText());
        assertThat(r1.detectedKeywords()).isEqualTo(r2.detectedKeywords());
    }

    @Test
    void tailor_emptyJobDescription_returnsEmptySuggestions() {
        TailoringRequest req = new TailoringRequest(
                "  ",
                List.of(new BulletWithId(UUID.randomUUID(), "Developed REST APIs.")),
                List.of());
        TailoringResponse res = provider.tailorResume(req);

        assertThat(res.suggestions()).isEmpty();
        assertThat(res.detectedKeywords()).isEmpty();
    }

    @Test
    void tailor_emptyBullets_returnsEmptySuggestions() {
        TailoringRequest req = new TailoringRequest(
                "Java Spring Boot", List.of(), List.of());
        TailoringResponse res = provider.tailorResume(req);

        assertThat(res.suggestions()).isEmpty();
    }

    @Test
    void tailor_providerNameSet() {
        TailoringRequest req = new TailoringRequest(
                "Java",
                List.of(new BulletWithId(UUID.randomUUID(), "Built APIs.")),
                List.of());
        TailoringResponse res = provider.tailorResume(req);

        assertThat(res.providerName()).contains("Demo");
    }
}
