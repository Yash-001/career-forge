package com.careerforge.backend.ai.dto;

import java.util.List;

/**
 * Result of a tailoring operation.
 * Contains suggestions only — no resume entities are modified.
 */
public record TailoringResponse(
        List<BulletSuggestion> suggestions,
        List<String> detectedKeywords,
        String providerName
) {}
