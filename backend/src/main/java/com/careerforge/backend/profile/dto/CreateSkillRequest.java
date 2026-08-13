package com.careerforge.backend.profile.dto;

import com.careerforge.backend.profile.domain.ProficiencyLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSkillRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) String category,
        ProficiencyLevel proficiency,
        int displayOrder
) {}
