package com.careerforge.backend.resume.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "resume_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false, updatable = false)
    private Resume resume;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(length = 255)
    private String title;

    @Column(name = "professional_summary", columnDefinition = "TEXT")
    private String professionalSummary;

    @OneToMany(mappedBy = "resumeVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private Set<ResumeExperience> experiences = new LinkedHashSet<>();

    @OneToMany(mappedBy = "resumeVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private Set<ResumeEducation> educations = new LinkedHashSet<>();

    @OneToMany(mappedBy = "resumeVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private Set<ResumeSkill> skills = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }
}
