package com.careerforge.backend.resume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ResumeEducationRequest(
        @NotBlank @Size(max = 255) String institutionName,
        @Size(max = 255) String degree,
        @Size(max = 255) String fieldOfStudy,
        @Size(max = 255) String location,
        LocalDate startDate,
        LocalDate endDate,
        @Size(max = 50) String grade,
        String description,
        int displayOrder
) {}
