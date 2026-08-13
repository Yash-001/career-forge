package com.careerforge.backend.profile.repository;

import com.careerforge.backend.profile.domain.MasterProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MasterProfileRepository extends JpaRepository<MasterProfile, UUID> {

    Optional<MasterProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
