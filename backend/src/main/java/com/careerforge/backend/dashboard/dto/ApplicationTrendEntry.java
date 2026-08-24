package com.careerforge.backend.dashboard.dto;

public record ApplicationTrendEntry(
        int year,
        int month,
        long count
) {}
