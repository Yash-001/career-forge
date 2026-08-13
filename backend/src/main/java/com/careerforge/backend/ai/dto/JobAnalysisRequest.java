package com.careerforge.backend.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record JobAnalysisRequest(
        @NotBlank String jobDescription,
        /** Optional: resume skill names already on the version being tailored. */
        java.util.List<String> resumeSkills
) {
    public JobAnalysisRequest {
        if (resumeSkills == null) resumeSkills = java.util.List.of();
    }
}
