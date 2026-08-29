package com.careerforge.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "Email is required.")
        @Email(message = "A valid email address is required.")
        @Size(max = 255, message = "Email must not exceed 255 characters.")
        String email,

        @NotBlank(message = "Password is required.")
        String password
) {}
