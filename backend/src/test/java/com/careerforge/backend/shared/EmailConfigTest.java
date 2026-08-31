package com.careerforge.backend.shared;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.careerforge.backend.shared.config.EmailConfig;
import com.careerforge.backend.shared.email.EmailService;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class EmailConfigTest {

    private final EmailConfig config = new EmailConfig();

    // ── 1. production + console → application starts ─────────────────────────

    @Test
    void production_consoleProvider_doesNotThrow() {
        assertThatCode(() -> config.emailService("console", "production"))
                .doesNotThrowAnyException();
    }

    @Test
    void production_consoleProvider_returnsEmailService() {
        EmailService service = config.emailService("console", "production");
        assertThat(service).isNotNull();
    }

    // ── 2. production + console → warning is logged ───────────────────────────

    @Test
    void production_consoleProvider_logsWarn() {
        Logger logger = (Logger) LoggerFactory.getLogger(EmailConfig.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            config.emailService("console", "production");
        } finally {
            logger.detachAppender(appender);
        }

        List<ILoggingEvent> warns = appender.list.stream()
                .filter(e -> e.getLevel() == Level.WARN)
                .toList();

        assertThat(warns).isNotEmpty();
        assertThat(warns.get(0).getFormattedMessage())
                .contains("ConsoleEmailProvider")
                .contains("NOT be delivered");
    }

    // ── 3. non-production + console → existing behavior unchanged ─────────────

    @Test
    void nonProduction_consoleProvider_doesNotThrow() {
        assertThatCode(() -> config.emailService("console", ""))
                .doesNotThrowAnyException();
    }

    @Test
    void nonProduction_consoleProvider_logsInfo_notWarn() {
        Logger logger = (Logger) LoggerFactory.getLogger(EmailConfig.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            config.emailService("console", "");
        } finally {
            logger.detachAppender(appender);
        }

        boolean hasWarn = appender.list.stream()
                .anyMatch(e -> e.getLevel() == Level.WARN);
        assertThat(hasWarn).isFalse();
    }

    @Test
    void nonProduction_nullEnv_doesNotThrow() {
        assertThatCode(() -> config.emailService("console", null))
                .doesNotThrowAnyException();
    }
}
