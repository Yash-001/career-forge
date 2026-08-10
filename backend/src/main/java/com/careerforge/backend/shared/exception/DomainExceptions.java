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
}
