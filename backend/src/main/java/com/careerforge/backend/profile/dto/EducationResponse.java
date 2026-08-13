package com.careerforge.backend.profile.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EducationResponse(
        UUID id,
        String institutionName,
        String degree,
        String fieldOfStudy,
        String location,
        LocalDate startDate,
        LocalDate endDate,
        String grade,
        String description,
        int displayOrder,
        Instant createdAt,
        Instant updatedAt
) {}
