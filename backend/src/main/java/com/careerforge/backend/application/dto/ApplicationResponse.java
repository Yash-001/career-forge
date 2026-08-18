package com.careerforge.backend.application.dto;

import com.careerforge.backend.application.domain.ApplicationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        String companyName,
        String jobTitle,
        LocalDate applicationDate,
        String jobUrl,
        ApplicationStatus status,
        UUID resumeVersionId,
        Instant createdAt,
        Instant updatedAt
) {}
