package com.careerforge.backend.resume.dto;

import java.time.Instant;
import java.util.UUID;

public record ResumeVersionSummaryResponse(
        UUID id,
        int versionNumber,
        String title,
        boolean isLatest,
        Instant createdAt
) {}
