package com.careerforge.backend.dashboard.dto;

import java.time.LocalDate;
import java.util.UUID;

public record RecentApplicationEntry(
        UUID id,
        String companyName,
        String jobTitle,
        LocalDate applicationDate,
        String status
) {}
