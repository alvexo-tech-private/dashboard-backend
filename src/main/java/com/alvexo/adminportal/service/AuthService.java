package com.alvexo.adminportal.service;

import com.alvexo.adminportal.dto.AdminUserSummaryDto;
import com.alvexo.adminportal.dto.LoginResponseDto;
import com.alvexo.adminportal.entity.AdminUser;
import com.alvexo.adminportal.entity.AdminUserStatus;
import com.alvexo.adminportal.exception.ApiException;
import com.alvexo.adminportal.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password.";
    private static final String INVALID_CURRENT_PASSWORD_MESSAGE = "Current password is incorrect.";

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;

    public LoginResponseDto login(String email, String password) {
        AdminUser admin = adminUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.unauthorized(INVALID_CREDENTIALS_MESSAGE));

        if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
            throw ApiException.unauthorized(INVALID_CREDENTIALS_MESSAGE);
        }
        if (admin.getStatus() != AdminUserStatus.ACTIVE) {
            throw ApiException.unauthorized(INVALID_CREDENTIALS_MESSAGE);
        }

        admin.setLastLoginAt(LocalDateTime.now());
        adminUserRepository.save(admin);

        String token = jwtService.generateAccessToken(admin);
        return LoginResponseDto.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .adminUser(toSummary(admin))
                .build();
    }

    /** The signed-in admin's current profile, so the portal picks up role changes without a new sign-in. */
    public AdminUserSummaryDto me(Long adminId) {
        AdminUser admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> ApiException.unauthorized(INVALID_CREDENTIALS_MESSAGE));
        return toSummary(admin);
    }

    // One transaction: the new password and its audit row are saved together or not at all.
    @Transactional
    public void changePassword(Long adminId, String currentPassword, String newPassword) {
        AdminUser admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> ApiException.unauthorized(INVALID_CREDENTIALS_MESSAGE));

        if (!passwordEncoder.matches(currentPassword, admin.getPasswordHash())) {
            throw ApiException.badRequest(INVALID_CURRENT_PASSWORD_MESSAGE);
        }
        if (passwordEncoder.matches(newPassword, admin.getPasswordHash())) {
            throw ApiException.badRequest("Choose a new password that is different from the current one.");
        }

        admin.setPasswordHash(passwordEncoder.encode(newPassword));
        admin.setMustChangePassword(false);
        adminUserRepository.save(admin);

        auditLogService.record(AdminUserService.ENTITY_TYPE, admin.getId(), admin.getId(), null, null,
                "PASSWORD_CHANGED", "Self-service password change.");
    }

    private static AdminUserSummaryDto toSummary(AdminUser admin) {
        return AdminUserSummaryDto.builder()
                .id(admin.getId())
                .email(admin.getEmail())
                .fullName(admin.getFullName())
                .role(admin.getRole())
                .mustChangePassword(admin.isMustChangePassword())
                .build();
    }
}
