package com.alvexo.adminportal.service;

import com.alvexo.adminportal.dto.AccountHistoryEntryDto;
import com.alvexo.adminportal.dto.PagedResponse;
import com.alvexo.adminportal.entity.AccountStatus;
import com.alvexo.adminportal.entity.AdminUser;
import com.alvexo.adminportal.entity.AuditLog;
import com.alvexo.adminportal.entity.BookingStatus;
import com.alvexo.adminportal.entity.Notification;
import com.alvexo.adminportal.entity.NotificationType;
import com.alvexo.adminportal.entity.SettlementStatus;
import com.alvexo.adminportal.entity.User;
import com.alvexo.adminportal.entity.UserRole;
import com.alvexo.adminportal.exception.ApiException;
import com.alvexo.adminportal.repository.AdminUserRepository;
import com.alvexo.adminportal.repository.AuditLogRepository;
import com.alvexo.adminportal.repository.BookingRepository;
import com.alvexo.adminportal.repository.NotificationRepository;
import com.alvexo.adminportal.repository.SettlementRepository;
import com.alvexo.adminportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Suspend / restore / soft-delete for rider, workshop (mechanic) and sales-agent
 * accounts: ADMIN-US-04 (workshops) and ADMIN-US-27 (workshops + sales agents),
 * extended to riders at the product owner's request (2026-11-14), overriding
 * Rider Management BR-10.
 *
 * Status lives in the booking API's own users.active / users.deleted columns
 * (see AccountStatus); every change writes an audit_log row and an in-app
 * notification to the affected user.
 */
@Service
@RequiredArgsConstructor
public class AccountLifecycleService {

    static final String ENTITY_TYPE = "USER_ACCOUNT";

    public static final String ACTION_SUSPEND = "ACCOUNT_SUSPENDED";
    public static final String ACTION_RESTORE = "ACCOUNT_RESTORED";
    public static final String ACTION_DELETE = "ACCOUNT_DELETED";

    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<UserRole> MANAGEABLE_ROLES =
            Set.of(UserRole.VEHICLE_USER, UserRole.MECHANIC, UserRole.SALES_REPRESENTATIVE);

    /** A booking in one of these states still needs both parties, so the account can't be deleted. */
    private static final List<BookingStatus> OPEN_BOOKING_STATUSES =
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS);

    /** Money still owed to (or held for) the workshop: ADMIN-US-04 BR-10 "unresolved financial obligations". */
    private static final List<SettlementStatus> UNRESOLVED_SETTLEMENT_STATUSES = List.of(
            SettlementStatus.AWAITING_PAYOUT, SettlementStatus.PROCESSING,
            SettlementStatus.FAILED, SettlementStatus.ON_HOLD);

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final SettlementRepository settlementRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final AdminUserRepository adminUserRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public AccountStatus suspend(Long userId, Long adminId, String reason) {
        User user = getManageableUser(userId);
        AccountStatus previous = AccountStatus.of(user);
        if (previous != AccountStatus.ACTIVE) {
            throw ApiException.conflict("INVALID_STATUS", "Only an active account can be suspended. This account is "
                    + previous.name().toLowerCase() + ".");
        }

        user.setActive(false);
        userRepository.save(user);

        record(user, previous, AccountStatus.SUSPENDED, ACTION_SUSPEND, adminId, reason);
        notify(user, NotificationType.ACCOUNT_SUSPENDED, "Your account has been suspended",
                "Your account has been suspended by the platform team. Reason: " + reason);
        return AccountStatus.SUSPENDED;
    }

    @Transactional
    public AccountStatus restore(Long userId, Long adminId, String reason) {
        User user = getManageableUser(userId);
        AccountStatus previous = AccountStatus.of(user);
        if (previous != AccountStatus.SUSPENDED) {
            throw ApiException.conflict("INVALID_STATUS", previous == AccountStatus.DELETED
                    ? "A deleted account cannot be restored."
                    : "This account is already active.");
        }

        // active = false is also how the booking app records a user's own
        // "delete my account" request (AuthService.requestAccountDeletion), so
        // only undo suspensions this portal made, and never silently reactivate
        // someone who asked to leave.
        boolean suspendedHere = auditLogRepository
                .findFirstByEntityTypeAndEntityIdOrderByCreatedAtDescIdDesc(ENTITY_TYPE, userId)
                .map(entry -> ACTION_SUSPEND.equals(entry.getAction()))
                .orElse(false);
        if (!suspendedHere) {
            throw ApiException.conflict("DEACTIVATED_OUTSIDE_PORTAL",
                    "This account was deactivated outside the Admin Portal (for example, the user asked to delete "
                            + "their account), so it can't be restored here.");
        }

        // Restoring only flips the status back; Listed / Trust / Pickup & Drop /
        // Advance Payment settings are untouched (ADMIN-US-04 BR-08).
        user.setActive(true);
        userRepository.save(user);

        record(user, previous, AccountStatus.ACTIVE, ACTION_RESTORE, adminId, reason);
        notify(user, NotificationType.ACCOUNT_RESTORED, "Your account has been restored",
                "Your account is active again. You can sign in and use the app as before.");
        return AccountStatus.ACTIVE;
    }

    @Transactional
    public AccountStatus delete(Long userId, Long adminId, String reason) {
        User user = getManageableUser(userId);
        AccountStatus previous = AccountStatus.of(user);
        if (previous == AccountStatus.DELETED) {
            throw ApiException.conflict("INVALID_STATUS", "This account is already deleted.");
        }
        ensureNoOpenObligations(user);

        // Soft delete only (cross-cutting rule): bookings, payments, settlements,
        // referrals and audit rows stay. active = false too, so every existing
        // active check in the booking API also blocks a deleted account.
        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        user.setActive(false);
        userRepository.save(user);

        record(user, previous, AccountStatus.DELETED, ACTION_DELETE, adminId, reason);
        notify(user, NotificationType.ACCOUNT_DELETED, "Your account has been closed",
                "Your account has been closed by the platform team. Reason: " + reason);
        return AccountStatus.DELETED;
    }

    @Transactional(readOnly = true)
    public PagedResponse<AccountHistoryEntryDto> getHistory(Long userId, int page, int size) {
        getManageableUser(userId);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<AuditLog> result = auditLogRepository.findByEntityTypeAndEntityId(ENTITY_TYPE, userId, pageable);

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

    private void ensureNoOpenObligations(User user) {
        Long id = user.getId();
        switch (user.getRole()) {
            case MECHANIC -> {
                long openBookings = bookingRepository.countByMechanicIdAndStatusIn(id, OPEN_BOOKING_STATUSES);
                if (openBookings > 0) {
                    throw ApiException.conflict("OPEN_BOOKINGS", "This workshop still has " + openBookings
                            + " open booking(s). Wait until they are completed or cancelled, or suspend the account instead.");
                }
                long unsettled = settlementRepository.countByMechanicIdAndStatusIn(id, UNRESOLVED_SETTLEMENT_STATUSES);
                if (unsettled > 0) {
                    throw ApiException.conflict("UNSETTLED_PAYMENTS", "This workshop still has " + unsettled
                            + " settlement(s) not yet paid out. Resolve them first, or suspend the account instead.");
                }
            }
            case VEHICLE_USER -> {
                long openBookings = bookingRepository.countByVehicleUserIdAndStatusIn(id, OPEN_BOOKING_STATUSES);
                if (openBookings > 0) {
                    throw ApiException.conflict("OPEN_BOOKINGS", "This rider still has " + openBookings
                            + " open booking(s). Wait until they are completed or cancelled, or suspend the account instead.");
                }
            }
            // Sales agents: no commission ledger exists yet (Epic 7), so there is
            // nothing to check; revisit when commissions are built.
            default -> { }
        }
    }

    private User getManageableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Account not found."));
        if (!MANAGEABLE_ROLES.contains(user.getRole())) {
            throw ApiException.forbidden("Only rider, workshop and sales agent accounts can be managed here.");
        }
        return user;
    }

    private void record(User user, AccountStatus previous, AccountStatus next, String action,
                        Long adminId, String reason) {
        auditLogService.record(ENTITY_TYPE, user.getId(), adminId, previous.name(), next.name(), action, reason);
    }

    private void notify(User user, NotificationType type, String title, String message) {
        notificationRepository.save(Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .notificationType(type)
                .isRead(false)
                .relatedEntityType(ENTITY_TYPE)
                .relatedEntityId(user.getId())
                .build());
    }
}
