package com.careerforge.backend.dashboard.dto;

public record ProfileSummary(
        boolean exists,
        boolean hasTitle,
        boolean hasSummary,
        boolean hasContactInfo,
        int completionPercent
) {}
