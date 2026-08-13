package com.careerforge.backend.ai;

import com.careerforge.backend.ai.dto.*;
import com.careerforge.backend.ai.provider.AIProvider;
import com.careerforge.backend.ai.service.AIService;
import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.resume.domain.ResumeExperience;
import com.careerforge.backend.resume.domain.ResumeSkill;
import com.careerforge.backend.resume.domain.ResumeVersion;
import com.careerforge.backend.resume.service.ResumeService;
import com.careerforge.backend.shared.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIServiceTest {

    @Mock AIProvider aiProvider;
    @Mock ResumeService resumeService;

    AIService aiService;

    UUID resumeId = UUID.randomUUID();
    UUID versionId = UUID.randomUUID();
    UUID expId = UUID.randomUUID();
    User user;
    ResumeVersion version;
    ResumeSkill skill;
    ResumeExperience exp;

    @BeforeEach
    void setUp() {
        aiService = new AIService(aiProvider, resumeService);

        user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .build();

        version = new ResumeVersion();

        skill = new ResumeSkill();
        skill.setName("Java");
        LinkedHashSet<ResumeSkill> skills = new LinkedHashSet<>();
        skills.add(skill);

        exp = new ResumeExperience();
        exp.setId(expId);
        exp.setDescription("Developed REST APIs using Spring Boot.");
        LinkedHashSet<ResumeExperience> experiences = new LinkedHashSet<>();
        experiences.add(exp);

        version.setSkills(skills);
        version.setExperiences(experiences);
        version.setEducations(new LinkedHashSet<>());
    }

    // ── analyzeJobDescription ─────────────────────────────────────────────────

    @Test
    void analyzeJobDescription_delegatesToProvider() {
        when(resumeService.getVersionById(user, resumeId, versionId)).thenReturn(version);
        JobAnalysisResponse mockResponse = new JobAnalysisResponse(
                "Backend Engineer", List.of("java"), List.of("java"),
                List.of(), List.of("java"), List.of(), "Demo AI");
        when(aiProvider.analyzeJobDescription(any())).thenReturn(mockResponse);

        JobAnalysisResponse result = aiService.analyzeJobDescription(
                user, resumeId, versionId, "Java Spring Boot backend engineer");

        assertThat(result).isEqualTo(mockResponse);
        verify(aiProvider, times(1)).analyzeJobDescription(any());
    }

    @Test
    void analyzeJobDescription_passesResumeSkillsToProvider() {
        when(resumeService.getVersionById(user, resumeId, versionId)).thenReturn(version);
        when(aiProvider.analyzeJobDescription(any())).thenReturn(
                new JobAnalysisResponse(null, List.of(), List.of(), List.of(), List.of(), List.of(), "Demo AI"));

        aiService.analyzeJobDescription(user, resumeId, versionId, "Java backend");

        ArgumentCaptor<JobAnalysisRequest> captor = ArgumentCaptor.forClass(JobAnalysisRequest.class);
        verify(aiProvider).analyzeJobDescription(captor.capture());
        assertThat(captor.getValue().resumeSkills()).contains("Java");
    }

    @Test
    void analyzeJobDescription_verifiesOwnershipViaResumeService() {
        when(resumeService.getVersionById(user, resumeId, versionId)).thenReturn(version);
        when(aiProvider.analyzeJobDescription(any())).thenReturn(
                new JobAnalysisResponse(null, List.of(), List.of(), List.of(), List.of(), List.of(), "Demo AI"));

        aiService.analyzeJobDescription(user, resumeId, versionId, "Java");

        verify(resumeService, times(1)).getVersionById(user, resumeId, versionId);
    }

    @Test
    void analyzeJobDescription_doesNotMutateResumeVersion() {
        when(resumeService.getVersionById(user, resumeId, versionId)).thenReturn(version);
        when(aiProvider.analyzeJobDescription(any())).thenReturn(
                new JobAnalysisResponse(null, List.of(), List.of(), List.of(), List.of(), List.of(), "Demo AI"));

        String originalSkillName = skill.getName();
        aiService.analyzeJobDescription(user, resumeId, versionId, "Java");

        assertThat(skill.getName()).isEqualTo(originalSkillName);
        verify(resumeService, never()).renameResume(any(), any(), any());
    }

    // ── tailorResume ──────────────────────────────────────────────────────────

    @Test
    void tailorResume_delegatesToProvider() {
        when(resumeService.getVersionById(user, resumeId, versionId)).thenReturn(version);
        TailoringResponse mockResponse = new TailoringResponse(List.of(), List.of(), "Demo AI");
        when(aiProvider.tailorResume(any())).thenReturn(mockResponse);

        TailoringResponse result = aiService.tailorResume(user, resumeId, versionId, "Java Spring Boot");

        assertThat(result).isEqualTo(mockResponse);
        verify(aiProvider, times(1)).tailorResume(any());
    }

    @Test
    void tailorResume_passesBulletsWithIdFromExperiences() {
        when(resumeService.getVersionById(user, resumeId, versionId)).thenReturn(version);
        when(aiProvider.tailorResume(any())).thenReturn(
                new TailoringResponse(List.of(), List.of(), "Demo AI"));

        aiService.tailorResume(user, resumeId, versionId, "Java Spring Boot");

        ArgumentCaptor<TailoringRequest> captor = ArgumentCaptor.forClass(TailoringRequest.class);
        verify(aiProvider).tailorResume(captor.capture());
        List<BulletWithId> bullets = captor.getValue().bullets();
        assertThat(bullets).hasSize(1);
        assertThat(bullets.get(0).experienceId()).isEqualTo(expId);
        assertThat(bullets.get(0).description()).isEqualTo("Developed REST APIs using Spring Boot.");
    }

    @Test
    void tailorResume_verifiesOwnershipViaResumeService() {
        when(resumeService.getVersionById(user, resumeId, versionId)).thenReturn(version);
        when(aiProvider.tailorResume(any())).thenReturn(
                new TailoringResponse(List.of(), List.of(), "Demo AI"));

        aiService.tailorResume(user, resumeId, versionId, "Java");

        verify(resumeService, times(1)).getVersionById(user, resumeId, versionId);
    }

    @Test
    void tailorResume_doesNotMutateResumeVersion() {
        when(resumeService.getVersionById(user, resumeId, versionId)).thenReturn(version);
        when(aiProvider.tailorResume(any())).thenReturn(
                new TailoringResponse(List.of(), List.of(), "Demo AI"));

        String originalDesc = exp.getDescription();
        aiService.tailorResume(user, resumeId, versionId, "Java");

        assertThat(exp.getDescription()).isEqualTo(originalDesc);
    }

    // ── acceptTailoring ───────────────────────────────────────────────────────

    @Test
    void acceptTailoring_verifiesOwnershipViaResumeService() {
        when(resumeService.getVersionById(user, resumeId, versionId)).thenReturn(version);
        ResumeVersion newVersion = new ResumeVersion();
        newVersion.setExperiences(new LinkedHashSet<>());
        newVersion.setEducations(new LinkedHashSet<>());
        newVersion.setSkills(new LinkedHashSet<>());
        when(resumeService.cloneVersionWithTailoring(any(), any(), any())).thenReturn(newVersion);

        AcceptTailoringRequest request = new AcceptTailoringRequest(
                List.of(new AcceptedSuggestion(expId, "Tailored text.")));
        aiService.acceptTailoring(user, resumeId, versionId, request);

        verify(resumeService, times(1)).getVersionById(user, resumeId, versionId);
    }

    @Test
    void acceptTailoring_delegatesToCloneVersionWithTailoring() {
        when(resumeService.getVersionById(user, resumeId, versionId)).thenReturn(version);
        ResumeVersion newVersion = new ResumeVersion();
        newVersion.setExperiences(new LinkedHashSet<>());
        newVersion.setEducations(new LinkedHashSet<>());
        newVersion.setSkills(new LinkedHashSet<>());
        when(resumeService.cloneVersionWithTailoring(any(), any(), any())).thenReturn(newVersion);

        AcceptTailoringRequest request = new AcceptTailoringRequest(
                List.of(new AcceptedSuggestion(expId, "Tailored text.")));
        ResumeVersion result = aiService.acceptTailoring(user, resumeId, versionId, request);

        assertThat(result).isEqualTo(newVersion);
        verify(resumeService, times(1)).cloneVersionWithTailoring(eq(resumeId), eq(version), any());
    }

    @Test
    void acceptTailoring_invalidExperienceId_throws422() {
        when(resumeService.getVersionById(user, resumeId, versionId)).thenReturn(version);

        UUID unknownExpId = UUID.randomUUID();
        AcceptTailoringRequest request = new AcceptTailoringRequest(
                List.of(new AcceptedSuggestion(unknownExpId, "Tailored text.")));

        assertThatThrownBy(() -> aiService.acceptTailoring(user, resumeId, versionId, request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(apiEx.getCode()).isEqualTo("INVALID_SUGGESTION");
                });
    }

    @Test
    void acceptTailoring_passesAcceptedMapToClone() {
        when(resumeService.getVersionById(user, resumeId, versionId)).thenReturn(version);
        ResumeVersion newVersion = new ResumeVersion();
        newVersion.setExperiences(new LinkedHashSet<>());
        newVersion.setEducations(new LinkedHashSet<>());
        newVersion.setSkills(new LinkedHashSet<>());
        when(resumeService.cloneVersionWithTailoring(any(), any(), any())).thenReturn(newVersion);

        String suggestedText = "Tailored bullet text.";
        AcceptTailoringRequest request = new AcceptTailoringRequest(
                List.of(new AcceptedSuggestion(expId, suggestedText)));
        aiService.acceptTailoring(user, resumeId, versionId, request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Map<UUID, String>> mapCaptor =
                ArgumentCaptor.forClass(java.util.Map.class);
        verify(resumeService).cloneVersionWithTailoring(eq(resumeId), eq(version), mapCaptor.capture());
        assertThat(mapCaptor.getValue()).containsEntry(expId, suggestedText);
    }

    // ── activeProviderName ────────────────────────────────────────────────────

    @Test
    void activeProviderName_delegatesToProvider() {
        when(aiProvider.providerName()).thenReturn("Demo AI (rule-based)");
        assertThat(aiService.activeProviderName()).isEqualTo("Demo AI (rule-based)");
    }
}
