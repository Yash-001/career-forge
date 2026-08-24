package com.careerforge.backend.dashboard.dto;

import java.time.Instant;
import java.util.UUID;

public record RecentResumeEntry(
        UUID id,
        String name,
        int latestVersionNumber,
        Instant updatedAt
) {}
