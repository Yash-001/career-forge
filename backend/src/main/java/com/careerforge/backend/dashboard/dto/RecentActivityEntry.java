package com.careerforge.backend.dashboard.dto;

import java.time.Instant;

public record RecentActivityEntry(
        String type,
        String label,
        String subLabel,
        String linkPath,
        Instant occurredAt
) {}
