package com.careerforge.backend.application.dto;

import com.careerforge.backend.application.domain.ApplicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateApplicationRequest(
        @NotBlank @Size(max = 255) String companyName,
        @NotBlank @Size(max = 255) String jobTitle,
        @NotNull LocalDate applicationDate,
        @Size(max = 2048)
        @Pattern(regexp = "^$|https?://.+", message = "jobUrl must be a valid URL")
        String jobUrl,
        ApplicationStatus status,
        UUID resumeVersionId
) {}
