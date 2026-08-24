package com.careerforge.backend.dashboard.dto;

import java.util.List;

public record DashboardSummary(
        ProfileSummary profile,
        ResumeSummary resumes,
        ApplicationSummary applications,
        SubscriptionSummary subscription,
        UsageSummary usage,
        QuickActions quickActions,
        AnalyticsSummary analytics,
        List<RecentActivityEntry> activity
) {}
