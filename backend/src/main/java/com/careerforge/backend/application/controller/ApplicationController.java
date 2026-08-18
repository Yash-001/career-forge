package com.careerforge.backend.application.controller;

import com.careerforge.backend.application.domain.Application;
import com.careerforge.backend.application.dto.ApplicationResponse;
import com.careerforge.backend.application.dto.CreateApplicationRequest;
import com.careerforge.backend.application.dto.UpdateApplicationRequest;
import com.careerforge.backend.application.service.ApplicationService;
import com.careerforge.backend.auth.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    public ResponseEntity<ApplicationResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateApplicationRequest request) {
        Application app = applicationService.create(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(app));
    }

    @GetMapping
    public ResponseEntity<List<ApplicationResponse>> list(@AuthenticationPrincipal User user) {
        List<ApplicationResponse> body = applicationService.listForUser(user)
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<ApplicationResponse> get(
            @AuthenticationPrincipal User user,
            @PathVariable UUID applicationId) {
        return ResponseEntity.ok(toResponse(applicationService.getOwned(user, applicationId)));
    }

    @PutMapping("/{applicationId}")
    public ResponseEntity<ApplicationResponse> update(
            @AuthenticationPrincipal User user,
            @PathVariable UUID applicationId,
            @Valid @RequestBody UpdateApplicationRequest request) {
        return ResponseEntity.ok(toResponse(applicationService.update(user, applicationId, request)));
    }

    @DeleteMapping("/{applicationId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable UUID applicationId) {
        applicationService.delete(user, applicationId);
        return ResponseEntity.noContent().build();
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private ApplicationResponse toResponse(Application app) {
        return new ApplicationResponse(
                app.getId(),
                app.getCompanyName(),
                app.getJobTitle(),
                app.getApplicationDate(),
                app.getJobUrl(),
                app.getStatus(),
                app.getResumeVersion() != null ? app.getResumeVersion().getId() : null,
                app.getCreatedAt(),
                app.getUpdatedAt()
        );
    }
}
