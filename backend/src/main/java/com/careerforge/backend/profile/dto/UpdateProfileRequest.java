package com.careerforge.backend.profile.dto;

import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UpdateProfileRequest(
        @Size(max = 50) String phone,
        @Size(max = 255) String location,
        @Size(max = 255) String professionalTitle,
        @Size(max = 5000) String professionalSummary,
        @URL @Size(max = 500) String linkedinUrl,
        @URL @Size(max = 500) String githubUrl,
        @URL @Size(max = 500) String portfolioUrl
) {}
