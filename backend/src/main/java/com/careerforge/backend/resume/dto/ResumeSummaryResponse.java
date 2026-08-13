package com.careerforge.backend.resume.dto;

import java.time.Instant;
import java.util.UUID;

public record ResumeSummaryResponse(
        UUID id,
        String name,
        int latestVersionNumber,
        Instant createdAt,
        Instant updatedAt
) {}
