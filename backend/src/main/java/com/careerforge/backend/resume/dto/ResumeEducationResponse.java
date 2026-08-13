package com.careerforge.backend.resume.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ResumeEducationResponse(
        UUID id,
        String institutionName,
        String degree,
        String fieldOfStudy,
        String location,
        LocalDate startDate,
        LocalDate endDate,
        String grade,
        String description,
        int displayOrder
) {}
