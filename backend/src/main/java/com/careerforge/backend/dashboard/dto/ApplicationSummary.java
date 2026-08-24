package com.careerforge.backend.dashboard.dto;

import java.util.List;

public record ApplicationSummary(
        long total,
        long applied,
        long interview,
        long offer,
        long rejected,
        List<RecentApplicationEntry> recentApplications
) {}
