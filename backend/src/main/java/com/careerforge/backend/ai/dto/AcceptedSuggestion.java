package com.careerforge.backend.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** A single accepted AI suggestion identifying the target experience and the desired text. */
public record AcceptedSuggestion(
        @NotNull UUID experienceId,
        @NotBlank String suggestedText
) {}
