package com.careerforge.backend.pdf.service;

import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.pdf.dto.ResumeVersionData;
import com.careerforge.backend.pdf.generator.PdfGenerator;
import com.careerforge.backend.resume.domain.ResumeVersion;
import com.careerforge.backend.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Owns: ownership enforcement (via ResumeService), data assembly, billing limit enforcement.
 * Delegates: PDF byte generation to PdfGenerator.
 */
@Service
@RequiredArgsConstructor
public class PdfExportService {

    private final ResumeService resumeService;
    private final PdfGenerator pdfGenerator;
    private final ExportLimitService exportLimitService;

    @Transactional(readOnly = true)
    public byte[] exportVersion(User user, UUID resumeId, UUID versionId) {
        exportLimitService.checkLimit(user);
        ResumeVersion version = resumeService.getVersionById(user, resumeId, versionId);
        ResumeVersionData data = toData(version);
        byte[] pdf = pdfGenerator.generate(data);
        exportLimitService.recordExport(user);
        return pdf;
    }

    /**
     * Returns a safe ASCII filename derived from the resume name and version title.
     * Strips characters that are unsafe in Content-Disposition headers.
     */
    @Transactional(readOnly = true)
    public String buildFilename(User user, UUID resumeId, UUID versionId) {
        ResumeVersion version = resumeService.getVersionById(user, resumeId, versionId);
        String resumeName = version.getResume().getName();
        String versionTitle = version.getTitle();

        String base = versionTitle != null && !versionTitle.isBlank()
                ? resumeName + " - " + versionTitle
                : resumeName;

        // Replace any character that is not alphanumeric, space, hyphen, or underscore
        String safe = base.replaceAll("[^a-zA-Z0-9 \\-_]", "").trim();
        if (safe.isBlank()) safe = "resume";

        return safe + ".pdf";
    }

    private ResumeVersionData toData(ResumeVersion v) {
        User user = v.getResume().getUser();
        String fullName = joinNonBlank(" ", user.getFirstName(), user.getLastName());

        List<ResumeVersionData.ExperienceData> experiences = v.getExperiences().stream()
                .map(e -> new ResumeVersionData.ExperienceData(
                        e.getJobTitle(),
                        e.getCompanyName(),
                        e.getLocation(),
                        formatDate(e.getStartDate()),
                        formatDate(e.getEndDate()),
                        e.isCurrentlyWorking(),
                        e.getDescription()
                )).toList();

        List<ResumeVersionData.EducationData> educations = v.getEducations().stream()
                .map(e -> new ResumeVersionData.EducationData(
                        e.getDegree(),
                        e.getFieldOfStudy(),
                        e.getInstitutionName(),
                        e.getLocation(),
                        formatDate(e.getStartDate()),
                        formatDate(e.getEndDate()),
                        e.getGrade()
                )).toList();

        List<ResumeVersionData.SkillData> skills = v.getSkills().stream()
                .map(s -> new ResumeVersionData.SkillData(
                        s.getName(),
                        s.getCategory(),
                        s.getProficiency() != null ? s.getProficiency().name() : null
                )).toList();

        return new ResumeVersionData(
                v.getResume().getName(),
                v.getTitle(),
                fullName,
                user.getEmail(),
                null,   // phone — on MasterProfile, wired in Phase 5C
                null,   // location — on MasterProfile
                null,   // linkedInUrl — on MasterProfile
                v.getProfessionalSummary(),
                experiences,
                educations,
                skills
        );
    }

    private String formatDate(LocalDate date) {
        if (date == null) return null;
        return date.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)
                + " " + date.getYear();
    }

    private String joinNonBlank(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                if (!sb.isEmpty()) sb.append(sep);
                sb.append(p);
            }
        }
        return sb.toString();
    }
}
