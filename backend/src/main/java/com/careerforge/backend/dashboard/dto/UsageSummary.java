package com.careerforge.backend.dashboard.dto;

public record UsageSummary(
        int pdfExportsUsed,
        int pdfExportsLimit,
        boolean atLimit
) {}
