package com.careerforge.backend.resume.dto;

import com.careerforge.backend.profile.domain.ProficiencyLevel;

import java.util.UUID;

public record ResumeSkillResponse(
        UUID id,
        String name,
        String category,
        ProficiencyLevel proficiency,
        int displayOrder
) {}
