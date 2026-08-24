package com.careerforge.backend.dashboard.dto;

public record DashboardSummary(
        ProfileSummary profile,
        ResumeSummary resumes,
        ApplicationSummary applications,
        SubscriptionSummary subscription,
        UsageSummary usage,
        QuickActions quickActions
) {}
