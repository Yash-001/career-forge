package com.careerforge.backend.shared.config;

import com.careerforge.backend.shared.email.ConsoleEmailProvider;
import com.careerforge.backend.shared.email.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class EmailConfig {

    @Bean
    public EmailService emailService(@Value("${app.email.provider:console}") String provider) {
        return switch (provider.toLowerCase()) {
            case "console" -> {
                log.info("Email provider: ConsoleEmailProvider (reset links logged to console)");
                yield new ConsoleEmailProvider();
            }
            default -> {
                log.warn("Unknown email provider '{}', falling back to ConsoleEmailProvider", provider);
                yield new ConsoleEmailProvider();
            }
        };
    }
}
