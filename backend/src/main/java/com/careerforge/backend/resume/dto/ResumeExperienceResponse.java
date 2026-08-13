package com.careerforge.backend.resume.dto;

import com.careerforge.backend.profile.domain.EmploymentType;

import java.time.LocalDate;
import java.util.UUID;

public record ResumeExperienceResponse(
        UUID id,
        String companyName,
        String jobTitle,
        String location,
        EmploymentType employmentType,
        LocalDate startDate,
        LocalDate endDate,
        boolean currentlyWorking,
        String description,
        int displayOrder
) {}
