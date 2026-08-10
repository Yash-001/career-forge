package com.careerforge.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Email is required.")
        @Email(message = "A valid email address is required.")
        String email,

        @NotBlank(message = "Password is required.")
        @Size(min = 8, message = "Password must be at least 8 characters.")
        @Pattern(regexp = ".*\\d.*", message = "Password must contain at least one number.")
        String password,

        @Size(max = 100, message = "First name must not exceed 100 characters.")
        String firstName,

        @Size(max = 100, message = "Last name must not exceed 100 characters.")
        String lastName
) {}
