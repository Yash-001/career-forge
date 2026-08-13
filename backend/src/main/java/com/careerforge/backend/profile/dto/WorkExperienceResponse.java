package com.careerforge.backend.profile.dto;

import com.careerforge.backend.profile.domain.EmploymentType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record WorkExperienceResponse(
        UUID id,
        String companyName,
        String jobTitle,
        String location,
        EmploymentType employmentType,
        LocalDate startDate,
        LocalDate endDate,
        boolean currentlyWorking,
        String description,
        int displayOrder,
        Instant createdAt,
        Instant updatedAt
) {}
