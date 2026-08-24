package com.careerforge.backend.dashboard.dto;

public record QuickActions(
        boolean canCreateResume,
        boolean canLogApplication,
        boolean canUpgrade
) {}
