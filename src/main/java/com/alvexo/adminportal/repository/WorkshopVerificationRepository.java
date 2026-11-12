package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.WorkshopVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Platform Verified (Trust) state per workshop. The queue itself is listed
 * from UserRepository, since a workshop that never submitted has no row here.
 */
public interface WorkshopVerificationRepository extends JpaRepository<WorkshopVerification, Long> {

    Optional<WorkshopVerification> findByMechanicId(Long mechanicId);

    long countByVerifiedTrue();
}
