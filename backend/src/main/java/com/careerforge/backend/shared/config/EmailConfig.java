package com.careerforge.backend.shared.config;

import com.careerforge.backend.shared.email.ConsoleEmailProvider;
import com.careerforge.backend.shared.email.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the active EmailService implementation.
 *
 * SEC-15: ConsoleEmailProvider (which logs reset links to stdout) must never
 * be active in a production environment. If app.env=production and the provider
 * is "console", startup is aborted with a clear error message.
 */
@Slf4j
@Configuration
public class EmailConfig {

    @Bean
    public EmailService emailService(
            @Value("${app.email.provider:console}") String provider,
            @Value("${app.env:}") String appEnv) {

        boolean isProduction = "production".equalsIgnoreCase(appEnv);

        return switch (provider.toLowerCase()) {
            case "console" -> {
                if (isProduction) {
                    throw new IllegalStateException(
                            "[SECURITY] ConsoleEmailProvider is not permitted in production. " +
                            "Set EMAIL_PROVIDER to a real email provider or set CAREERFORGE_ENV " +
                            "to a non-production value.");
                }
                log.info("Email provider: ConsoleEmailProvider (reset links logged to console)");
                yield new ConsoleEmailProvider();
            }
            default -> {
                log.warn("Unknown email provider '{}', falling back to ConsoleEmailProvider", provider);
                if (isProduction) {
                    throw new IllegalStateException(
                            "[SECURITY] Unknown email provider '" + provider + "' in production. " +
                            "Configure a supported email provider.");
                }
                yield new ConsoleEmailProvider();
            }
        };
    }
}
