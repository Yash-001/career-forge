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
 * ConsoleEmailProvider is suitable for development and demo environments only.
 * It does NOT deliver real email — reset links are printed to the application log.
 *
 * When app.env=production and EMAIL_PROVIDER=console, the application logs a
 * prominent WARN and continues starting. No real email will be delivered.
 * A real email provider must be configured before using CareerForge as a
 * customer-facing commercial application.
 *
 * SEC-15 (relaxed): Hard startup failure removed — the application must not
 * crash when no SMTP provider is available, as no SMTP implementation exists yet.
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
                    log.warn("[EMAIL] ConsoleEmailProvider is active in a production environment. " +
                             "Emails will NOT be delivered. " +
                             "This configuration is unsuitable for real password-reset email delivery. " +
                             "Set EMAIL_PROVIDER to a real email provider before serving real users.");
                } else {
                    log.info("Email provider: ConsoleEmailProvider (reset links logged to console)");
                }
                yield new ConsoleEmailProvider();
            }
            default -> {
                log.warn("Unknown email provider '{}', falling back to ConsoleEmailProvider", provider);
                if (isProduction) {
                    log.warn("[EMAIL] Unknown email provider '{}' in production — falling back to ConsoleEmailProvider. " +
                             "Emails will NOT be delivered.", provider);
                }
                yield new ConsoleEmailProvider();
            }
        };
    }
}
