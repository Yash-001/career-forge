package com.careerforge.backend.demo;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Fails fast at startup if demo mode is enabled in a production environment.
 * PRD SEC-11: CAREERFORGE_DEMO_MODE=true must be blocked when CAREERFORGE_ENV=production.
 */
@Component
public class DemoGuard {

    private static final Logger log = LoggerFactory.getLogger(DemoGuard.class);

    private final boolean demoMode;
    private final String env;

    public DemoGuard(
            @Value("${app.demo.mode:false}") boolean demoMode,
            @Value("${app.env:}") String env) {
        this.demoMode = demoMode;
        this.env = env;
    }

    @PostConstruct
    public void enforce() {
        if (demoMode && "production".equalsIgnoreCase(env)) {
            throw new IllegalStateException(
                    "[CAREERFORGE] FATAL: CAREERFORGE_DEMO_MODE=true is not allowed when " +
                    "CAREERFORGE_ENV=production. Set CAREERFORGE_DEMO_MODE=false or remove it.");
        }
        if (demoMode) {
            log.warn("=================================================================");
            log.warn("  DEMO MODE ACTIVE — not for production use.");
            log.warn("  Demo seed data will be loaded on startup.");
            log.warn("=================================================================");
        }
    }

    public boolean isDemoMode() {
        return demoMode;
    }
}
