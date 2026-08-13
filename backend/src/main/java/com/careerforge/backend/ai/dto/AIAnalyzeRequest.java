package com.careerforge.backend.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * HTTP request body for POST /api/v1/ai/resumes/{resumeId}/versions/{versionId}/analyze.
 * Only the job description is accepted from the client.
 * Resume skills are populated server-side from the authenticated user's resume version.
 */
public record AIAnalyzeRequest(
        @NotBlank(message = "Job description must not be blank")
        @Size(max = 10000, message = "Job description must not exceed 10000 characters")
        String jobDescription
) {}
