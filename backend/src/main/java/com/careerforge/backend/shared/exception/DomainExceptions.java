package com.careerforge.backend.shared.exception;

import org.springframework.http.HttpStatus;

public final class DomainExceptions {

    private DomainExceptions() {}

    public static ApiException emailAlreadyExists() {
        return new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS",
                "An account with this email address already exists.");
    }

    public static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                "The email or password is incorrect.");
    }

    public static ApiException invalidOrExpiredToken() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_OR_EXPIRED_TOKEN",
                "The reset token is invalid, expired, or has already been used.");
    }

    public static ApiException invalidRefreshToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN",
                "The refresh token is missing, invalid, or expired.");
    }

    public static ApiException userNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
                "The requested user could not be found.");
    }

    public static ApiException profileNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND",
                "No profile found for this user.");
    }

    public static ApiException workExperienceNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "WORK_EXPERIENCE_NOT_FOUND",
                "Work experience entry not found.");
    }

    public static ApiException educationNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "EDUCATION_NOT_FOUND",
                "Education entry not found.");
    }

    public static ApiException skillNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "SKILL_NOT_FOUND",
                "Skill entry not found.");
    }

    public static ApiException resumeNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "RESUME_NOT_FOUND",
                "Resume not found.");
    }

    public static ApiException resumeVersionNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "RESUME_VERSION_NOT_FOUND",
                "Resume version not found.");
    }

    public static ApiException resumeNameBlank() {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Resume name cannot be blank.");
    }

    public static ApiException resumeExperienceNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "RESUME_EXPERIENCE_NOT_FOUND",
                "Resume experience entry not found.");
    }

    public static ApiException resumeEducationNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "RESUME_EDUCATION_NOT_FOUND",
                "Resume education entry not found.");
    }

    public static ApiException resumeSkillNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "RESUME_SKILL_NOT_FOUND",
                "Resume skill entry not found.");
    }

    public static ApiException invalidSuggestion() {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_SUGGESTION",
                "One or more AI suggestions are no longer valid for this resume version.");
    }

    public static ApiException exportLimitExceeded() {
        return new ApiException(HttpStatus.PAYMENT_REQUIRED, "PDF_EXPORT_LIMIT_EXCEEDED",
                "You have reached the 3 PDF export limit for this month. Upgrade to Pro for unlimited exports.");
    }

    public static ApiException applicationNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "APPLICATION_NOT_FOUND",
                "Job application not found.");
    }

    public static ApiException noActiveSubscription() {
        return new ApiException(HttpStatus.BAD_REQUEST, "NO_ACTIVE_SUBSCRIPTION",
                "No active subscription found for this user.");
    }

    public static ApiException alreadyPro() {
        return new ApiException(HttpStatus.CONFLICT, "ALREADY_PRO",
                "This account already has an active Pro subscription.");
    }

    public static ApiException billingProviderError(String detail) {
        return new ApiException(HttpStatus.BAD_GATEWAY, "BILLING_PROVIDER_ERROR",
                "The billing provider returned an error: " + detail);
    }
}
