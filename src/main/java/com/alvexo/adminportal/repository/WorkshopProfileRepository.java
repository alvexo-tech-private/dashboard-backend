package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.WorkshopProfile;
import com.alvexo.adminportal.entity.WorkshopStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkshopProfileRepository extends JpaRepository<WorkshopProfile, Long> {

    Optional<WorkshopProfile> findByMechanicId(Long mechanicId);

    long countByStatusAndSuspendedFalse(WorkshopStatus status);
}
