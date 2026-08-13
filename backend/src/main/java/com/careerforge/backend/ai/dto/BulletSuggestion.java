package com.careerforge.backend.ai.dto;

import java.util.List;
import java.util.UUID;

/**
 * A single tailored bullet suggestion.
 * experienceId identifies the source ResumeExperience so the frontend can
 * send it back in AcceptedSuggestion without relying on array position.
 */
public record BulletSuggestion(
        UUID experienceId,
        String originalText,
        String suggestedText,
        List<String> matchedKeywords,
        String rationale
) {}
