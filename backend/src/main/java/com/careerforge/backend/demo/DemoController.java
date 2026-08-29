package com.careerforge.backend.demo;

import com.careerforge.backend.auth.dto.AuthResponse;
import com.careerforge.backend.auth.dto.LoginRequest;
import com.careerforge.backend.auth.service.AuthService;
import com.careerforge.backend.shared.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demo-only endpoint: POST /api/v1/demo/login
 *
 * Returns a valid JWT for the seeded demo user without requiring the caller
 * to know the password. Only available when app.demo.mode=true.
 *
 * Security properties:
 * - Blocked at runtime when demo mode is off (403).
 * - Does NOT bypass authentication — delegates to AuthService.login.
 * - Demo credentials are configured via environment variables, not hardcoded.
 */
@RestController
@RequestMapping("/api/v1/demo")
public class DemoController {

    private final DemoGuard demoGuard;
    private final DemoProperties demoProperties;
    private final AuthService authService;

    public DemoController(DemoGuard demoGuard, DemoProperties demoProperties, AuthService authService) {
        this.demoGuard = demoGuard;
        this.demoProperties = demoProperties;
        this.authService = authService;
    }

    /**
     * Issues a JWT for the demo user.
     * Returns 403 if demo mode is not active.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> demoLogin() {
        if (!demoGuard.isDemoMode()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "DEMO_DISABLED",
                    "Demo mode is not enabled on this server.");
        }
        AuthResponse response = authService.login(
                new LoginRequest(demoProperties.userEmail(), demoProperties.userPassword()));
        return ResponseEntity.ok(response);
    }
}
