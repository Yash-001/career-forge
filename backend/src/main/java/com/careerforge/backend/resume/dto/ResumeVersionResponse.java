package com.careerforge.backend.resume.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ResumeVersionResponse(
        UUID id,
        int versionNumber,
        String title,
        String professionalSummary,
        boolean isLatest,
        List<ResumeExperienceResponse> experiences,
        List<ResumeEducationResponse> educations,
        List<ResumeSkillResponse> skills,
        Instant createdAt
) {}
