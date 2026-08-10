package com.careerforge.backend.shared;

import com.careerforge.backend.shared.email.ConsoleEmailProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class ConsoleEmailProviderTest {

    private final ConsoleEmailProvider provider = new ConsoleEmailProvider();

    @Test
    void sendPasswordResetEmail_doesNotThrow() {
        assertThatCode(() ->
                provider.sendPasswordResetEmail("user@example.com",
                        "http://localhost:5173/reset-password?token=abc123"))
                .doesNotThrowAnyException();
    }

    @Test
    void sendPasswordResetEmail_worksWithoutExternalCredentials() {
        // No SMTP config, no API key — must complete without error
        assertThatCode(() ->
                provider.sendPasswordResetEmail("another@example.com", "http://example.com/reset"))
                .doesNotThrowAnyException();
    }
}
