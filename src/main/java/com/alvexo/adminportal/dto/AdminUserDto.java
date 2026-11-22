package com.alvexo.adminportal.dto;

import com.alvexo.adminportal.entity.AdminRole;
import com.alvexo.adminportal.entity.AdminUserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** One row of the Admin Users screen (ADMIN-US-29). Never carries the password hash. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserDto {

    private Long id;
    private String email;
    private String fullName;
    private AdminRole role;
    private AdminUserStatus status;
    /** True until the admin replaces the temporary password they were given. */
    private boolean mustChangePassword;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
