package com.careerforge.backend.profile.dto;

import com.careerforge.backend.profile.domain.ProficiencyLevel;

import java.time.Instant;
import java.util.UUID;

public record SkillResponse(
        UUID id,
        String name,
        String category,
        ProficiencyLevel proficiency,
        int displayOrder,
        Instant createdAt,
        Instant updatedAt
) {}
