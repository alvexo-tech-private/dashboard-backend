package com.alvexo.adminportal.service;

import com.alvexo.adminportal.dto.TemporaryPasswordDto;
import com.alvexo.adminportal.entity.AdminRole;
import com.alvexo.adminportal.entity.AdminUser;
import com.alvexo.adminportal.entity.AdminUserStatus;
import com.alvexo.adminportal.exception.ApiException;
import com.alvexo.adminportal.repository.AdminUserRepository;
import com.alvexo.adminportal.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Pure unit tests: repositories and the encoder are mocked, nothing touches a database. */
class AdminUserServiceTest {

    private static final long ACTING_ID = 1L;
    private static final long TARGET_ID = 2L;

    private AdminUserRepository adminUserRepository;
    private AuditLogService auditLogService;
    private PasswordEncoder passwordEncoder;
    private AdminUserService service;

    @BeforeEach
    void setUp() {
        adminUserRepository = mock(AdminUserRepository.class);
        auditLogService = mock(AuditLogService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "hash:" + inv.getArgument(0));
        when(adminUserRepository.save(any(AdminUser.class))).thenAnswer(inv -> {
            AdminUser a = inv.getArgument(0);
            if (a.getId() == null) a.setId(99L);
            return a;
        });
        service = new AdminUserService(adminUserRepository, mock(AuditLogRepository.class), auditLogService,
                passwordEncoder);
    }

    private AdminUser target(AdminRole role, AdminUserStatus status) {
        AdminUser admin = AdminUser.builder().id(TARGET_ID).email("t@alvexo.com").fullName("Target")
                .role(role).status(status).passwordHash("old").build();
        when(adminUserRepository.findById(TARGET_ID)).thenReturn(Optional.of(admin));
        return admin;
    }

    private void activeSuperAdmins(long count) {
        when(adminUserRepository.countByRoleAndStatus(AdminRole.SUPER_ADMIN, AdminUserStatus.ACTIVE)).thenReturn(count);
    }

    @Test
    void createStoresHashedTemporaryPasswordAndAudits() {
        TemporaryPasswordDto result = service.create("  New.Admin@Alvexo.com ", " New Admin ",
                AdminRole.FINANCE_ADMIN, ACTING_ID, " ");

        ArgumentCaptor<AdminUser> saved = ArgumentCaptor.forClass(AdminUser.class);
        verify(adminUserRepository).save(saved.capture());
        AdminUser admin = saved.getValue();
        assertEquals("new.admin@alvexo.com", admin.getEmail());
        assertEquals("New Admin", admin.getFullName());
        assertEquals(AdminUserStatus.ACTIVE, admin.getStatus());
        assertTrue(admin.isMustChangePassword());
        assertEquals("hash:" + result.getTemporaryPassword(), admin.getPasswordHash());
        assertTrue(result.getAdminUser().isMustChangePassword());
        verify(auditLogService).record(eq(AdminUserService.ENTITY_TYPE), eq(99L), eq(ACTING_ID), isNull(),
                eq("FINANCE_ADMIN"), eq(AdminUserService.ACTION_CREATED), isNull());
    }

    @Test
    void createRejectsDuplicateEmail() {
        when(adminUserRepository.existsByEmailIgnoreCase("dup@alvexo.com")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.create("Dup@alvexo.com", "Dup", AdminRole.OPERATIONS_ADMIN, ACTING_ID, null));
        assertEquals("EMAIL_IN_USE", ex.getErrorCode());
        verify(adminUserRepository, never()).save(any());
    }

    @Test
    void changeRoleAuditsPreviousAndNewRole() {
        AdminUser admin = target(AdminRole.OPERATIONS_ADMIN, AdminUserStatus.ACTIVE);

        service.changeRole(TARGET_ID, AdminRole.FINANCE_ADMIN, ACTING_ID, "Moved to finance");

        assertEquals(AdminRole.FINANCE_ADMIN, admin.getRole());
        verify(auditLogService).record(AdminUserService.ENTITY_TYPE, TARGET_ID, ACTING_ID, "OPERATIONS_ADMIN",
                "FINANCE_ADMIN", AdminUserService.ACTION_ROLE_CHANGED, "Moved to finance");
    }

    @Test
    void cannotChangeOwnRole() {
        AdminUser self = AdminUser.builder().id(ACTING_ID).role(AdminRole.SUPER_ADMIN)
                .status(AdminUserStatus.ACTIVE).build();
        when(adminUserRepository.findById(ACTING_ID)).thenReturn(Optional.of(self));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.changeRole(ACTING_ID, AdminRole.FINANCE_ADMIN, ACTING_ID, "x"));
        assertEquals("OWN_ACCOUNT", ex.getErrorCode());
    }

    @Test
    void cannotDemoteTheLastActiveSuperAdmin() {
        target(AdminRole.SUPER_ADMIN, AdminUserStatus.ACTIVE);
        activeSuperAdmins(1);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.changeRole(TARGET_ID, AdminRole.FINANCE_ADMIN, ACTING_ID, "x"));
        assertEquals("LAST_SUPER_ADMIN", ex.getErrorCode());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void canDemoteASuperAdminWhenAnotherIsActive() {
        AdminUser admin = target(AdminRole.SUPER_ADMIN, AdminUserStatus.ACTIVE);
        activeSuperAdmins(2);

        service.changeRole(TARGET_ID, AdminRole.OPERATIONS_ADMIN, ACTING_ID, "Step down");

        assertEquals(AdminRole.OPERATIONS_ADMIN, admin.getRole());
    }

    @Test
    void disableAndEnableFlipStatusAndAudit() {
        AdminUser admin = target(AdminRole.FINANCE_ADMIN, AdminUserStatus.ACTIVE);

        service.disable(TARGET_ID, ACTING_ID, "Left the company");
        assertEquals(AdminUserStatus.DISABLED, admin.getStatus());
        verify(auditLogService).record(AdminUserService.ENTITY_TYPE, TARGET_ID, ACTING_ID, "ACTIVE", "DISABLED",
                AdminUserService.ACTION_DISABLED, "Left the company");

        service.enable(TARGET_ID, ACTING_ID, "Rejoined");
        assertEquals(AdminUserStatus.ACTIVE, admin.getStatus());
        verify(auditLogService).record(AdminUserService.ENTITY_TYPE, TARGET_ID, ACTING_ID, "DISABLED", "ACTIVE",
                AdminUserService.ACTION_ENABLED, "Rejoined");
    }

    @Test
    void cannotDisableSelf() {
        AdminUser self = AdminUser.builder().id(ACTING_ID).role(AdminRole.SUPER_ADMIN)
                .status(AdminUserStatus.ACTIVE).build();
        when(adminUserRepository.findById(ACTING_ID)).thenReturn(Optional.of(self));

        ApiException ex = assertThrows(ApiException.class, () -> service.disable(ACTING_ID, ACTING_ID, "x"));
        assertEquals("OWN_ACCOUNT", ex.getErrorCode());
    }

    @Test
    void cannotDisableTheLastActiveSuperAdmin() {
        target(AdminRole.SUPER_ADMIN, AdminUserStatus.ACTIVE);
        activeSuperAdmins(1);

        ApiException ex = assertThrows(ApiException.class, () -> service.disable(TARGET_ID, ACTING_ID, "x"));
        assertEquals("LAST_SUPER_ADMIN", ex.getErrorCode());
    }

    @Test
    void resetPasswordIssuesNewTemporaryPassword() {
        AdminUser admin = target(AdminRole.OPERATIONS_ADMIN, AdminUserStatus.ACTIVE);

        TemporaryPasswordDto result = service.resetPassword(TARGET_ID, ACTING_ID, "Forgot password");

        assertEquals("hash:" + result.getTemporaryPassword(), admin.getPasswordHash());
        assertTrue(admin.isMustChangePassword());
        verify(auditLogService).record(AdminUserService.ENTITY_TYPE, TARGET_ID, ACTING_ID, null, null,
                AdminUserService.ACTION_PASSWORD_RESET, "Forgot password");
    }

    @Test
    void temporaryPasswordsAreStrongAndDifferent() {
        String a = service.generateTemporaryPassword();
        String b = service.generateTemporaryPassword();
        assertEquals(14, a.length());
        assertTrue(a.chars().anyMatch(Character::isUpperCase));
        assertTrue(a.chars().anyMatch(Character::isLowerCase));
        assertTrue(a.chars().anyMatch(Character::isDigit));
        assertFalse(a.chars().allMatch(Character::isLetterOrDigit));
        assertNotEquals(a, b);
    }
}
