package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.WorkshopRegistrationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface WorkshopRegistrationRequestRepository extends JpaRepository<WorkshopRegistrationRequest, Long> {

    Optional<WorkshopRegistrationRequest> findByMechanicId(Long mechanicId);

    @Query("SELECT r.mechanic.id FROM WorkshopRegistrationRequest r")
    Set<Long> findAllMechanicIds();

    @Query("SELECT r FROM WorkshopRegistrationRequest r JOIN r.mechanic m WHERE " +
            "r.status IN (com.alvexo.adminportal.entity.WorkshopRegistrationRequestStatus.PENDING, " +
            "com.alvexo.adminportal.entity.WorkshopRegistrationRequestStatus.ADDITIONAL_INFO_REQUESTED) AND (" +
            ":search IS NULL OR :search = '' " +
            "OR LOWER(m.workshopName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.mobileNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.city) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<WorkshopRegistrationRequest> searchQueue(@Param("search") String search, Pageable pageable);

    @Query("SELECT r FROM WorkshopRegistrationRequest r JOIN r.mechanic m WHERE " +
            ":search IS NULL OR :search = '' " +
            "OR LOWER(m.workshopName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.mobileNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.city) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<WorkshopRegistrationRequest> searchAll(@Param("search") String search, Pageable pageable);
}
