package com.alvexo.adminportal.repository;

import com.alvexo.adminportal.entity.AdminRole;
import com.alvexo.adminportal.entity.AdminUser;
import com.alvexo.adminportal.entity.AdminUserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long>, JpaSpecificationExecutor<AdminUser> {

    Optional<AdminUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByRoleAndStatus(AdminRole role, AdminUserStatus status);
}
