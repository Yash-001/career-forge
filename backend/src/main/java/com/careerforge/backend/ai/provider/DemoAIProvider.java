package com.careerforge.backend.ai.provider;

import com.careerforge.backend.ai.dto.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Demo AI Provider — deterministic, rule-based.
 *
 * This is NOT an LLM. It uses keyword matching against a curated vocabulary
 * to simulate AI analysis. It requires no API key, no network access, and
 * no external service. It is the default provider for local development and
 * demo environments.
 *
 * Registered as a named bean; the active AIProvider bean is selected by AIConfig.
 */
@Component("demoAIProvider")
public class DemoAIProvider implements AIProvider {

    private static final String PROVIDER_NAME = "Demo AI (rule-based)";

    // ── Known vocabulary ──────────────────────────────────────────────────────

    private static final Map<String, String> ROLE_SIGNALS = Map.ofEntries(
            Map.entry("backend", "Backend Engineer"),
            Map.entry("frontend", "Frontend Engineer"),
            Map.entry("full stack", "Full Stack Engineer"),
            Map.entry("fullstack", "Full Stack Engineer"),
            Map.entry("devops", "DevOps Engineer"),
            Map.entry("data engineer", "Data Engineer"),
            Map.entry("machine learning", "Machine Learning Engineer"),
            Map.entry("android", "Android Engineer"),
            Map.entry("ios", "iOS Engineer"),
            Map.entry("mobile", "Mobile Engineer"),
            Map.entry("cloud", "Cloud Engineer"),
            Map.entry("security", "Security Engineer"),
            Map.entry("qa", "QA Engineer"),
            Map.entry("test engineer", "QA Engineer"),
            Map.entry("product manager", "Product Manager"),
            Map.entry("data scientist", "Data Scientist"),
            Map.entry("software engineer", "Software Engineer"),
            Map.entry("software developer", "Software Developer")
    );

    private static final Set<String> KNOWN_TECHNOLOGIES = Set.of(
            "java", "spring", "spring boot", "spring mvc", "spring security",
            "python", "django", "flask", "fastapi",
            "javascript", "typescript", "node.js", "nodejs", "express",
            "react", "vue", "angular", "next.js", "nuxt",
            "kotlin", "scala", "go", "rust", "c#", ".net", "php",
            "postgresql", "mysql", "mongodb", "redis", "elasticsearch",
            "aws", "gcp", "azure", "docker", "kubernetes", "terraform",
            "kafka", "rabbitmq", "graphql", "rest", "rest apis", "grpc",
            "git", "ci/cd", "jenkins", "github actions", "gradle", "maven",
            "hibernate", "jpa", "jdbc", "flyway", "liquibase",
            "junit", "mockito", "pytest", "jest", "vitest",
            "html", "css", "tailwind", "sass",
            "linux", "bash", "microservices", "oauth", "jwt"
    );

    private static final Set<String> SOFT_KEYWORDS = Set.of(
            "agile", "scrum", "kanban", "collaboration", "communication",
            "leadership", "mentoring", "problem solving", "ownership",
            "cross-functional", "stakeholder", "deadline", "delivery"
    );

    private static final List<String> RESPONSIBILITY_PATTERNS = List.of(
            "design", "develop", "build", "implement", "maintain", "deploy",
            "optimize", "collaborate", "review", "test", "debug", "architect",
            "integrate", "monitor", "document", "lead", "mentor", "scale"
    );

    // ── AIProvider ────────────────────────────────────────────────────────────

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public JobAnalysisResponse analyzeJobDescription(JobAnalysisRequest request) {
        if (request.jobDescription() == null || request.jobDescription().isBlank()) {
            return new JobAnalysisResponse(
                    null, List.of(), List.of(), List.of(), List.of(), List.of(), PROVIDER_NAME);
        }

        String lower = request.jobDescription().toLowerCase(Locale.ROOT);

        String detectedRole = detectRole(lower);
        List<String> technologies = extractTechnologies(lower);
        List<String> softKeywords = extractSoftKeywords(lower);
        List<String> responsibilities = extractResponsibilities(lower);

        List<String> allKeywords = new ArrayList<>();
        allKeywords.addAll(technologies);
        allKeywords.addAll(softKeywords);

        List<String> resumeSkillsLower = request.resumeSkills().stream()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .toList();

        List<String> matched = technologies.stream()
                .filter(t -> resumeSkillsLower.stream()
                        .anyMatch(rs -> rs.contains(t) || t.contains(rs)))
                .distinct()
                .toList();

        List<String> missing = technologies.stream()
                .filter(t -> matched.stream().noneMatch(m -> m.equalsIgnoreCase(t)))
                .distinct()
                .toList();

        return new JobAnalysisResponse(
                detectedRole,
                deduplicate(allKeywords),
                technologies,
                responsibilities,
                matched,
                missing,
                PROVIDER_NAME
        );
    }

    @Override
    public TailoringResponse tailorResume(TailoringRequest request) {
        if (request.jobDescription() == null || request.jobDescription().isBlank()) {
            return new TailoringResponse(List.of(), List.of(), PROVIDER_NAME);
        }

        String lower = request.jobDescription().toLowerCase(Locale.ROOT);
        List<String> detectedKeywords = extractTechnologies(lower);
        detectedKeywords.addAll(extractSoftKeywords(lower));
        detectedKeywords = deduplicate(detectedKeywords);

        List<BulletSuggestion> suggestions = new ArrayList<>();
        for (BulletWithId bullet : request.bullets()) {
            suggestions.add(tailorBullet(bullet, detectedKeywords));
        }

        return new TailoringResponse(suggestions, detectedKeywords, PROVIDER_NAME);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private String detectRole(String lower) {
        for (Map.Entry<String, String> entry : ROLE_SIGNALS.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "Software Engineer";
    }

    private List<String> extractTechnologies(String lower) {
        return KNOWN_TECHNOLOGIES.stream()
                .filter(lower::contains)
                .sorted()
                .collect(Collectors.toList());
    }

    private List<String> extractSoftKeywords(String lower) {
        return SOFT_KEYWORDS.stream()
                .filter(lower::contains)
                .sorted()
                .collect(Collectors.toList());
    }

    private List<String> extractResponsibilities(String lower) {
        return RESPONSIBILITY_PATTERNS.stream()
                .filter(lower::contains)
                .sorted()
                .collect(Collectors.toList());
    }

    private BulletSuggestion tailorBullet(BulletWithId bullet, List<String> detectedKeywords) {
        String original = bullet.description();
        if (original == null || original.isBlank()) {
            return new BulletSuggestion(bullet.experienceId(), original, original, List.of(), "No changes suggested.");
        }

        String lowerBullet = original.toLowerCase(Locale.ROOT);
        List<String> matched = detectedKeywords.stream()
                .filter(lowerBullet::contains)
                .toList();

        if (matched.isEmpty()) {
            String suggested = original.stripTrailing();
            if (!suggested.endsWith(".")) suggested += ".";
            suggested += " Aligned with job requirements.";
            return new BulletSuggestion(bullet.experienceId(), original, suggested, List.of(),
                    "No direct keyword overlap found; generic alignment phrase appended.");
        }

        String keywordList = String.join(", ", matched);
        String suggested = original.stripTrailing();
        if (!suggested.endsWith(".")) suggested += ".";
        suggested += " Demonstrates proficiency in " + keywordList + ".";

        return new BulletSuggestion(bullet.experienceId(), original, suggested, matched,
                "Matched keywords from job description: " + keywordList + ".");
    }

    private List<String> deduplicate(List<String> list) {
        return list.stream().distinct().sorted().collect(Collectors.toList());
    }
}
