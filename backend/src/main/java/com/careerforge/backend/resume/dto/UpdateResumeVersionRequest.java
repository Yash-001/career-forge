package com.careerforge.backend.resume.dto;

import jakarta.validation.constraints.Size;

public record UpdateResumeVersionRequest(
        @Size(max = 255) String title,
        @Size(max = 5000) String professionalSummary
) {}
