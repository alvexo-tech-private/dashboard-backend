package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.WorkshopVerification;
import com.alvexo.adminportal.entity.WorkshopVerificationStatus;
import com.alvexo.adminportal.entity.WorkshopVerificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface WorkshopVerificationRepository extends JpaRepository<WorkshopVerification, Long> {

    Optional<WorkshopVerification> findByMechanicIdAndVerificationType(Long mechanicId, WorkshopVerificationType verificationType);

    @Query("SELECT v.mechanic.id FROM WorkshopVerification v WHERE v.verificationType = :verificationType")
    Set<Long> findAllMechanicIdsByVerificationType(@Param("verificationType") WorkshopVerificationType verificationType);

    long countByVerificationTypeAndStatus(WorkshopVerificationType verificationType, WorkshopVerificationStatus status);

    @Query("SELECT v FROM WorkshopVerification v JOIN v.mechanic m WHERE " +
            "v.status IN (com.alvexo.adminportal.entity.WorkshopVerificationStatus.PENDING, " +
            "com.alvexo.adminportal.entity.WorkshopVerificationStatus.ADDITIONAL_INFO_REQUESTED) AND (" +
            ":verificationType IS NULL OR v.verificationType = :verificationType) AND (" +
            ":search IS NULL OR :search = '' " +
            "OR LOWER(m.workshopName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.mobileNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.city) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<WorkshopVerification> searchQueue(@Param("verificationType") WorkshopVerificationType verificationType,
                                            @Param("search") String search, Pageable pageable);

    @Query("SELECT v FROM WorkshopVerification v JOIN v.mechanic m WHERE " +
            "(:verificationType IS NULL OR v.verificationType = :verificationType) AND (" +
            ":search IS NULL OR :search = '' " +
            "OR LOWER(m.workshopName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.mobileNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.city) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<WorkshopVerification> searchAll(@Param("verificationType") WorkshopVerificationType verificationType,
                                          @Param("search") String search, Pageable pageable);
}
