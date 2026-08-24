package com.careerforge.backend.dashboard.dto;

import java.util.List;

public record AnalyticsSummary(
        long pipelineApplied,
        long pipelineInterview,
        long pipelineOffer,
        long pipelineRejected,
        List<ApplicationTrendEntry> trend
) {}
