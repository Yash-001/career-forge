package com.careerforge.backend.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** HTTP request body for the accept-tailoring endpoint. */
public record AcceptTailoringRequest(
        @NotEmpty List<@Valid AcceptedSuggestion> acceptedSuggestions
) {}
