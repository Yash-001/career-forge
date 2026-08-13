package com.careerforge.backend.profile.dto;

import java.time.Instant;
import java.util.UUID;

public record ProfileResponse(
        UUID id,
        String phone,
        String location,
        String professionalTitle,
        String professionalSummary,
        String linkedinUrl,
        String githubUrl,
        String portfolioUrl,
        Instant createdAt,
        Instant updatedAt
) {}
