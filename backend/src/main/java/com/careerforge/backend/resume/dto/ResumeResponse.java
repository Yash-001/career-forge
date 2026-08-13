package com.careerforge.backend.resume.dto;

import java.time.Instant;
import java.util.UUID;

public record ResumeResponse(
        UUID id,
        String name,
        ResumeVersionSummaryResponse latestVersion,
        Instant createdAt,
        Instant updatedAt
) {}
