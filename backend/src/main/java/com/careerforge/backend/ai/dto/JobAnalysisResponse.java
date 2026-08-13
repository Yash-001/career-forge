package com.careerforge.backend.ai.dto;

import java.util.List;

/**
 * Result of analysing a job description.
 * Read-only — no resume entities are modified.
 */
public record JobAnalysisResponse(
        String detectedRole,
        List<String> keywords,
        List<String> technologies,
        List<String> responsibilities,
        List<String> matchedResumeSkills,
        List<String> missingSkills,
        String providerName
) {}
