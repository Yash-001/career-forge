package com.careerforge.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(

        @NotBlank(message = "Token is required.")
        String token,

        @NotBlank(message = "Password is required.")
        @Size(min = 8, message = "Password must be at least 8 characters.")
        @Pattern(regexp = ".*\\d.*", message = "Password must contain at least one number.")
        String newPassword
) {}
