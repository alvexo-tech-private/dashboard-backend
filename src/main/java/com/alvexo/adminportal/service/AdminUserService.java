package com.alvexo.adminportal.service;

import com.alvexo.adminportal.dto.AccountHistoryEntryDto;
import com.alvexo.adminportal.dto.AdminUserDto;
import com.alvexo.adminportal.dto.PagedResponse;
import com.alvexo.adminportal.dto.TemporaryPasswordDto;
import com.alvexo.adminportal.entity.AdminRole;
import com.alvexo.adminportal.entity.AdminUser;
import com.alvexo.adminportal.entity.AdminUserStatus;
import com.alvexo.adminportal.entity.AuditLog;
import com.alvexo.adminportal.exception.ApiException;
import com.alvexo.adminportal.repository.AdminUserRepository;
import com.alvexo.adminportal.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Admin user management (ADMIN-US-29), Super Admin only (enforced on the
 * controller). Every change writes an audit_log row under entity type
 * {@link #ENTITY_TYPE}, the same type AuthService uses for self-service
 * password changes, so an admin's history is one timeline.
 *
 * Guard rails so the portal can't be locked out: an admin can't disable
 * themselves or change their own role, and the last active Super Admin
 * can't be disabled or moved to another role.
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    public static final String ENTITY_TYPE = "AdminUser";

    public static final String ACTION_CREATED = "ADMIN_CREATED";
    public static final String ACTION_ROLE_CHANGED = "ADMIN_ROLE_CHANGED";
    public static final String ACTION_DISABLED = "ADMIN_DISABLED";
    public static final String ACTION_ENABLED = "ADMIN_ENABLED";
    public static final String ACTION_PASSWORD_RESET = "ADMIN_PASSWORD_RESET";

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "fullName", "email", "role", "status", "lastLoginAt", "createdAt");

    // No 0/O, 1/l/I, so a password read out over the phone isn't misheard.
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "@#$%&*!?";
    private static final int TEMP_PASSWORD_LENGTH = 14;

    private final AdminUserRepository adminUserRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    @Transactional(readOnly = true)
    public PagedResponse<AdminUserDto> list(int page, int size, String search, AdminRole role,
                                            AdminUserStatus status, String sortBy, String sortDir) {
        String sortField = SORTABLE_FIELDS.contains(sortBy) ? sortBy : "fullName";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(direction, sortField).and(Sort.by("id")));

        Specification<AdminUser> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("fullName")), like),
                        cb.like(cb.lower(root.get("email")), like)));
            }
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };

        return PagedResponse.from(adminUserRepository.findAll(spec, pageable), AdminUserService::toDto);
    }

    @Transactional
    public TemporaryPasswordDto create(String email, String fullName, AdminRole role, Long actingAdminId,
                                       String reason) {
        String normalizedEmail = email.trim().toLowerCase();
        if (adminUserRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw ApiException.conflict("EMAIL_IN_USE", "An admin with this email already exists.");
        }

        String temporaryPassword = generateTemporaryPassword();
        AdminUser admin = adminUserRepository.save(AdminUser.builder()
                .email(normalizedEmail)
                .fullName(fullName.trim())
                .role(role)
                .status(AdminUserStatus.ACTIVE)
                .passwordHash(passwordEncoder.encode(temporaryPassword))
                .mustChangePassword(true)
                .build());

        auditLogService.record(ENTITY_TYPE, admin.getId(), actingAdminId, null, role.name(), ACTION_CREATED,
                blankToNull(reason));
        return new TemporaryPasswordDto(toDto(admin), temporaryPassword);
    }

    @Transactional
    public AdminUserDto changeRole(Long adminId, AdminRole newRole, Long actingAdminId, String reason) {
        AdminUser admin = get(adminId);
        if (admin.getId().equals(actingAdminId)) {
            throw ApiException.conflict("OWN_ACCOUNT", "You can't change your own role. Ask another Super Admin.");
        }
        AdminRole previous = admin.getRole();
        if (previous == newRole) {
            throw ApiException.conflict("NO_CHANGE", "This admin already has that role.");
        }
        if (previous == AdminRole.SUPER_ADMIN && admin.getStatus() == AdminUserStatus.ACTIVE) {
            ensureAnotherActiveSuperAdmin();
        }

        admin.setRole(newRole);
        adminUserRepository.save(admin);
        auditLogService.record(ENTITY_TYPE, admin.getId(), actingAdminId, previous.name(), newRole.name(),
                ACTION_ROLE_CHANGED, reason);
        return toDto(admin);
    }

    @Transactional
    public AdminUserDto disable(Long adminId, Long actingAdminId, String reason) {
        AdminUser admin = get(adminId);
        if (admin.getId().equals(actingAdminId)) {
            throw ApiException.conflict("OWN_ACCOUNT", "You can't disable your own account.");
        }
        if (admin.getStatus() == AdminUserStatus.DISABLED) {
            throw ApiException.conflict("INVALID_STATUS", "This admin is already disabled.");
        }
        if (admin.getRole() == AdminRole.SUPER_ADMIN) {
            ensureAnotherActiveSuperAdmin();
        }

        admin.setStatus(AdminUserStatus.DISABLED);
        adminUserRepository.save(admin);
        auditLogService.record(ENTITY_TYPE, admin.getId(), actingAdminId, AdminUserStatus.ACTIVE.name(),
                AdminUserStatus.DISABLED.name(), ACTION_DISABLED, reason);
        return toDto(admin);
    }

    @Transactional
    public AdminUserDto enable(Long adminId, Long actingAdminId, String reason) {
        AdminUser admin = get(adminId);
        if (admin.getStatus() == AdminUserStatus.ACTIVE) {
            throw ApiException.conflict("INVALID_STATUS", "This admin is already active.");
        }

        admin.setStatus(AdminUserStatus.ACTIVE);
        adminUserRepository.save(admin);
        auditLogService.record(ENTITY_TYPE, admin.getId(), actingAdminId, AdminUserStatus.DISABLED.name(),
                AdminUserStatus.ACTIVE.name(), ACTION_ENABLED, reason);
        return toDto(admin);
    }

    @Transactional
    public TemporaryPasswordDto resetPassword(Long adminId, Long actingAdminId, String reason) {
        AdminUser admin = get(adminId);
        if (admin.getId().equals(actingAdminId)) {
            throw ApiException.conflict("OWN_ACCOUNT",
                    "Use Account Settings to change your own password.");
        }

        String temporaryPassword = generateTemporaryPassword();
        admin.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        admin.setMustChangePassword(true);
        adminUserRepository.save(admin);
        auditLogService.record(ENTITY_TYPE, admin.getId(), actingAdminId, null, null, ACTION_PASSWORD_RESET, reason);
        return new TemporaryPasswordDto(toDto(admin), temporaryPassword);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AccountHistoryEntryDto> getHistory(Long adminId, int page, int size) {
        get(adminId);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<AuditLog> result = auditLogRepository.findByEntityTypeAndEntityId(ENTITY_TYPE, adminId, pageable);

        Map<Long, String> adminNames = adminUserRepository
                .findAllById(result.getContent().stream().map(AuditLog::getPerformedByAdminId).distinct().toList())
                .stream().collect(Collectors.toMap(AdminUser::getId, AdminUser::getFullName, (a, b) -> a));

        return PagedResponse.from(result, entry -> AccountHistoryEntryDto.builder()
                .id(entry.getId())
                .action(entry.getAction())
                .previousStatus(entry.getPreviousStatus())
                .newStatus(entry.getNewStatus())
                .reason(entry.getReason())
                .performedBy(adminNames.getOrDefault(entry.getPerformedByAdminId(), "Unknown admin"))
                .performedAt(entry.getCreatedAt())
                .build());
    }

    /** At least 14 characters with one of each character class, shuffled. */
    String generateTemporaryPassword() {
        String all = UPPER + LOWER + DIGITS + SYMBOLS;
        List<Character> chars = new ArrayList<>(TEMP_PASSWORD_LENGTH);
        chars.add(pick(UPPER));
        chars.add(pick(LOWER));
        chars.add(pick(DIGITS));
        chars.add(pick(SYMBOLS));
        while (chars.size() < TEMP_PASSWORD_LENGTH) {
            chars.add(pick(all));
        }
        java.util.Collections.shuffle(chars, random);
        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
        chars.forEach(sb::append);
        return sb.toString();
    }

    private char pick(String alphabet) {
        return alphabet.charAt(random.nextInt(alphabet.length()));
    }

    private void ensureAnotherActiveSuperAdmin() {
        if (adminUserRepository.countByRoleAndStatus(AdminRole.SUPER_ADMIN, AdminUserStatus.ACTIVE) <= 1) {
            throw ApiException.conflict("LAST_SUPER_ADMIN",
                    "This is the only active Super Admin. Make another admin a Super Admin first.");
        }
    }

    private AdminUser get(Long adminId) {
        return adminUserRepository.findById(adminId)
                .orElseThrow(() -> ApiException.notFound("Admin user not found."));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static AdminUserDto toDto(AdminUser admin) {
        return AdminUserDto.builder()
                .id(admin.getId())
                .email(admin.getEmail())
                .fullName(admin.getFullName())
                .role(admin.getRole())
                .status(admin.getStatus())
                .mustChangePassword(admin.isMustChangePassword())
                .lastLoginAt(admin.getLastLoginAt())
                .createdAt(admin.getCreatedAt())
                .build();
    }
}
