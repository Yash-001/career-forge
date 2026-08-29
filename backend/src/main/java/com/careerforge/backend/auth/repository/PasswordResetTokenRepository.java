package com.careerforge.backend.auth.repository;

import com.careerforge.backend.auth.domain.PasswordResetToken;
import com.careerforge.backend.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.user = :user")
    void deleteAllByUser(@Param("user") User user);

    /**
     * Returns only non-used, non-expired tokens.
     * Used by resetPassword to avoid a full table scan (SEC fix).
     * The result set is small (at most one valid token per user at any time
     * because forgotPassword deletes previous tokens before creating a new one).
     */
    @Query("SELECT t FROM PasswordResetToken t WHERE t.used = false AND t.expiresAt > :now")
    List<PasswordResetToken> findValidTokens(@Param("now") Instant now);

    /** Convenience overload using the current instant. */
    default List<PasswordResetToken> findValidTokens() {
        return findValidTokens(Instant.now());
    }
}
