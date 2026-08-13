package com.careerforge.backend.ai.dto;

import java.util.UUID;

/** Provider-layer DTO pairing a resume experience ID with its bullet text. */
public record BulletWithId(UUID experienceId, String description) {}
