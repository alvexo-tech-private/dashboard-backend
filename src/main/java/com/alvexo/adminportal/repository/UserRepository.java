package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.User;
import com.alvexo.adminportal.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.role = :role AND (" +
            ":search IS NULL OR :search = '' " +
            "OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.mobileNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.workshopName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchMechanics(@Param("role") UserRole role, @Param("search") String search, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = :role AND (" +
            ":search IS NULL OR :search = '' " +
            "OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.mobileNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchVehicleUsers(@Param("role") UserRole role, @Param("search") String search, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = :role AND (" +
            ":search IS NULL OR :search = '' " +
            "OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.mobileNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.referralCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchSalesAgents(@Param("role") UserRole role, @Param("search") String search, Pageable pageable);

    long countByRoleAndDeletedFalse(UserRole role);

    long countByRoleAndDeletedTrue(UserRole role);

    long countByRoleAndActiveFalseAndDeletedFalse(UserRole role);
}
