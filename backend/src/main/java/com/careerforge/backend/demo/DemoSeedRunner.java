package com.careerforge.backend.demo;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Triggers demo seed on startup when demo mode is active.
 * DemoGuard has already validated that we are not in production.
 */
@Component
public class DemoSeedRunner implements ApplicationRunner {

    private final DemoGuard demoGuard;
    private final DemoSeedService demoSeedService;

    public DemoSeedRunner(DemoGuard demoGuard, DemoSeedService demoSeedService) {
        this.demoGuard = demoGuard;
        this.demoSeedService = demoSeedService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (demoGuard.isDemoMode()) {
            demoSeedService.seed();
        }
    }
}
