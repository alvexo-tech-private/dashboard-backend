package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.WorkshopOnboarding;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Registration requests = submitted onboardings. The queue status comes from
 * workshop_profiles.status, not onboarding.decision: the booking API moves a
 * resubmitted workshop back to PENDING without clearing an old MORE_INFO decision.
 */
public interface WorkshopOnboardingRepository extends JpaRepository<WorkshopOnboarding, Long> {

    @Query("SELECT o FROM WorkshopOnboarding o JOIN o.mechanic m JOIN WorkshopProfile p ON p.mechanic = m WHERE " +
            "o.submitted = true AND (" +
            "p.status = com.alvexo.adminportal.entity.WorkshopStatus.PENDING " +
            "OR (p.status = com.alvexo.adminportal.entity.WorkshopStatus.IN_PROGRESS " +
            "AND o.decision = com.alvexo.adminportal.entity.OnboardingDecision.MORE_INFO)) AND (" +
            ":search IS NULL OR :search = '' " +
            "OR LOWER(m.workshopName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.mobileNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.city) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<WorkshopOnboarding> searchQueue(@Param("search") String search, Pageable pageable);

    @Query("SELECT o FROM WorkshopOnboarding o JOIN o.mechanic m WHERE o.submitted = true AND (" +
            ":search IS NULL OR :search = '' " +
            "OR LOWER(m.workshopName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.mobileNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.city) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<WorkshopOnboarding> searchAll(@Param("search") String search, Pageable pageable);
}
