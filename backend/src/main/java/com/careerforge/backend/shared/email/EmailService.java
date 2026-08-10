package com.careerforge.backend.shared.email;

public interface EmailService {

    /**
     * Sends a password reset link to the given email address.
     * Implementations must never throw on delivery failure — log and continue.
     */
    void sendPasswordResetEmail(String toEmail, String resetLink);
}
