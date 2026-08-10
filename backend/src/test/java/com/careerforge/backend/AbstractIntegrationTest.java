package com.careerforge.backend;

import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests.
 * Uses the "test" Spring profile which points to the local PostgreSQL
 * instance on port 5433 (started via docker-compose).
 * No Docker-in-Docker or Testcontainers required.
 */
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {
}
