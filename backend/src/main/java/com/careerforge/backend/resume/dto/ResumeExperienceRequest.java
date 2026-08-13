package com.careerforge.backend.resume.dto;

import com.careerforge.backend.profile.domain.EmploymentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ResumeExperienceRequest(
        @NotBlank @Size(max = 255) String companyName,
        @NotBlank @Size(max = 255) String jobTitle,
        @Size(max = 255) String location,
        EmploymentType employmentType,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        boolean currentlyWorking,
        String description,
        int displayOrder
) {}
