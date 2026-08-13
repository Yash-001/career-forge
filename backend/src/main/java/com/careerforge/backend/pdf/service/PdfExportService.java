package com.careerforge.backend.pdf.service;

import com.careerforge.backend.auth.domain.User;
import com.careerforge.backend.pdf.dto.ResumeVersionData;
import com.careerforge.backend.pdf.generator.PdfGenerator;
import com.careerforge.backend.resume.domain.ResumeVersion;
import com.careerforge.backend.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Owns: ownership enforcement (via ResumeService), data assembly, billing limit enforcement (Phase 5B).
 * Delegates: PDF byte generation to PdfGenerator.
 */
@Service
@RequiredArgsConstructor
public class PdfExportService {

    private final ResumeService resumeService;
    private final PdfGenerator pdfGenerator;

    public byte[] exportVersion(User user, UUID resumeId, UUID versionId) {
        ResumeVersion version = resumeService.getVersionById(user, resumeId, versionId);
        ResumeVersionData data = toData(version);
        return pdfGenerator.generate(data);
    }

    private ResumeVersionData toData(ResumeVersion v) {
        User user = v.getResume().getUser();

        String fullName = joinNonBlank(" ", user.getFirstName(), user.getLastName());

        var profile = v.getResume().getUser();  // contact info comes from User; profile fields from MasterProfile loaded separately if needed

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
                null,    // phone — on MasterProfile, loaded in Phase 5B when profile is fetched
                null,    // location — on MasterProfile
                null,    // linkedInUrl — on MasterProfile
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
