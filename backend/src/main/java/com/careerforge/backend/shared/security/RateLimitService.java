package com.careerforge.backend.shared.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-process rate limiter for authentication endpoints.
 *
 * Limits are applied per IP address using a fixed-window counter.
 * The window resets after {@code windowSeconds} seconds from the first request.
 *
 * SINGLE-INSTANCE LIMITATION: This implementation is in-process only.
 * In a horizontally scaled deployment, each instance maintains its own counters.
 * For multi-instance deployments, replace with a distributed store (e.g. Redis).
 * This is acceptable for the MVP single-instance target (PRD NFR-11).
 *
 * SEC-10: Rate limiting on login, register, and password-reset endpoints.
 */
@Service
public class RateLimitService {

    /** Maximum attempts per IP per window. */
    public static final int MAX_ATTEMPTS = 20;

    /** Window duration in seconds. */
    public static final long WINDOW_SECONDS = 60;

    private record Window(AtomicInteger count, Instant windowStart) {}

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * Records an attempt for the given key (typically "endpoint:ip").
     *
     * @return true if the request is allowed, false if the limit is exceeded
     */
    public boolean isAllowed(String key) {
        Instant now = Instant.now();
        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || now.isAfter(existing.windowStart().plusSeconds(WINDOW_SECONDS))) {
                return new Window(new AtomicInteger(1), now);
            }
            existing.count().incrementAndGet();
            return existing;
        });
        return window.count().get() <= MAX_ATTEMPTS;
    }

    /** Visible for testing. */
    public void clear() {
        windows.clear();
    }
}
