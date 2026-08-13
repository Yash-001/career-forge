package com.careerforge.backend.profile.controller;

import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.profile.dto.*;
import com.careerforge.backend.profile.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    // ── Profile ───────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(profileService.getProfile(user));
    }

    @PutMapping
    public ResponseEntity<ProfileResponse> upsertProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(profileService.upsertProfile(user, request));
    }

    // ── Work Experience ───────────────────────────────────────────────────────

    @GetMapping("/experience")
    public ResponseEntity<List<WorkExperienceResponse>> getExperiences(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(profileService.getExperiences(user));
    }

    @PostMapping("/experience")
    public ResponseEntity<WorkExperienceResponse> createExperience(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateWorkExperienceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profileService.createExperience(user, request));
    }

    @PutMapping("/experience/{id}")
    public ResponseEntity<WorkExperienceResponse> updateExperience(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkExperienceRequest request) {
        return ResponseEntity.ok(profileService.updateExperience(user, id, request));
    }

    @DeleteMapping("/experience/{id}")
    public ResponseEntity<Void> deleteExperience(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        profileService.deleteExperience(user, id);
        return ResponseEntity.noContent().build();
    }

    // ── Education ─────────────────────────────────────────────────────────────

    @GetMapping("/education")
    public ResponseEntity<List<EducationResponse>> getEducations(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(profileService.getEducations(user));
    }

    @PostMapping("/education")
    public ResponseEntity<EducationResponse> createEducation(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateEducationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profileService.createEducation(user, request));
    }

    @PutMapping("/education/{id}")
    public ResponseEntity<EducationResponse> updateEducation(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEducationRequest request) {
        return ResponseEntity.ok(profileService.updateEducation(user, id, request));
    }

    @DeleteMapping("/education/{id}")
    public ResponseEntity<Void> deleteEducation(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        profileService.deleteEducation(user, id);
        return ResponseEntity.noContent().build();
    }

    // ── Skills ────────────────────────────────────────────────────────────────

    @GetMapping("/skills")
    public ResponseEntity<List<SkillResponse>> getSkills(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(profileService.getSkills(user));
    }

    @PostMapping("/skills")
    public ResponseEntity<SkillResponse> createSkill(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateSkillRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profileService.createSkill(user, request));
    }

    @PutMapping("/skills/{id}")
    public ResponseEntity<SkillResponse> updateSkill(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSkillRequest request) {
        return ResponseEntity.ok(profileService.updateSkill(user, id, request));
    }

    @DeleteMapping("/skills/{id}")
    public ResponseEntity<Void> deleteSkill(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        profileService.deleteSkill(user, id);
        return ResponseEntity.noContent().build();
    }
}
