package com.careerforge.backend.dashboard.dto;

import java.util.List;

public record ResumeSummary(
        long resumeCount,
        long versionCount,
        List<RecentResumeEntry> recentResumes
) {}
