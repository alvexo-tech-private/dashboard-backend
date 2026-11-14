package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.User;
import com.alvexo.adminportal.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    String MECHANIC_SEARCH = "(:search IS NULL OR :search = '' " +
            "OR LOWER(m.workshopName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.mobileNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.city) LIKE LOWER(CONCAT('%', :search, '%')))";

    /**
     * Directory status filter: ALL, or one AccountStatus (ACTIVE / SUSPENDED /
     * DELETED) derived from active + deleted, the same way AccountStatus.of does.
     */
    String STATUS_FILTER = "(:status = 'ALL' " +
            "OR (:status = 'ACTIVE' AND u.active = true AND u.deleted = false) " +
            "OR (:status = 'SUSPENDED' AND u.active = false AND u.deleted = false) " +
            "OR (:status = 'DELETED' AND u.deleted = true))";

    /** ADMIN-US-06 queue: every non-deleted workshop not yet Platform Verified (no workshop_verifications row, or verified = false). */
    @Query(value = "SELECT m FROM User m LEFT JOIN WorkshopVerification v ON v.mechanic = m " +
            "WHERE m.role = com.alvexo.adminportal.entity.UserRole.MECHANIC AND m.deleted = false " +
            "AND (v.id IS NULL OR v.verified = false) AND " + MECHANIC_SEARCH,
            countQuery = "SELECT COUNT(m) FROM User m LEFT JOIN WorkshopVerification v ON v.mechanic = m " +
            "WHERE m.role = com.alvexo.adminportal.entity.UserRole.MECHANIC AND m.deleted = false " +
            "AND (v.id IS NULL OR v.verified = false) AND " + MECHANIC_SEARCH)
    Page<User> searchVerificationQueue(@Param("search") String search, Pageable pageable);

    /** ADMIN-US-06 "all" view: every non-deleted workshop, whatever its Platform Verified state. */
    @Query("SELECT m FROM User m WHERE m.role = com.alvexo.adminportal.entity.UserRole.MECHANIC " +
            "AND m.deleted = false AND " + MECHANIC_SEARCH)
    Page<User> searchVerificationAll(@Param("search") String search, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = :role AND " + STATUS_FILTER + " AND (" +
            ":search IS NULL OR :search = '' " +
            "OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.mobileNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.workshopName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchMechanics(@Param("role") UserRole role, @Param("search") String search,
                          @Param("status") String status, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = :role AND " + STATUS_FILTER + " AND (" +
            ":search IS NULL OR :search = '' " +
            "OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.mobileNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchVehicleUsers(@Param("role") UserRole role, @Param("search") String search,
                          @Param("status") String status, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = :role AND " + STATUS_FILTER + " AND (" +
            ":search IS NULL OR :search = '' " +
            "OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.mobileNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.referralCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchSalesAgents(@Param("role") UserRole role, @Param("search") String search,
                          @Param("status") String status, Pageable pageable);

    long countByRoleAndDeletedFalse(UserRole role);

    long countByRoleAndDeletedTrue(UserRole role);

    long countByRoleAndActiveFalseAndDeletedFalse(UserRole role);

    List<User> findByRoleAndDeletedFalse(UserRole role);
}
