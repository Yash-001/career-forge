package com.careerforge.backend;

import com.careerforge.backend.shared.security.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests.
 * Uses the "test" Spring profile which points to the local PostgreSQL
 * instance on port 5433 (started via docker-compose).
 * No Docker-in-Docker or Testcontainers required.
 *
 * Clears the in-process RateLimitService before each test so rate-limit
 * counters from one test class do not bleed into another.
 */
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired(required = false)
    private RateLimitService rateLimitService;

    @BeforeEach
    void clearRateLimiter() {
        if (rateLimitService != null) {
            rateLimitService.clear();
        }
    }
}
