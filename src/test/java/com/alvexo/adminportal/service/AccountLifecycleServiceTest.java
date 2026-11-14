package com.alvexo.adminportal.service;

import com.alvexo.adminportal.entity.AccountStatus;
import com.alvexo.adminportal.entity.AuditLog;
import com.alvexo.adminportal.entity.Notification;
import com.alvexo.adminportal.entity.NotificationType;
import com.alvexo.adminportal.entity.User;
import com.alvexo.adminportal.entity.UserRole;
import com.alvexo.adminportal.exception.ApiException;
import com.alvexo.adminportal.repository.AdminUserRepository;
import com.alvexo.adminportal.repository.AuditLogRepository;
import com.alvexo.adminportal.repository.BookingRepository;
import com.alvexo.adminportal.repository.NotificationRepository;
import com.alvexo.adminportal.repository.SettlementRepository;
import com.alvexo.adminportal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pure unit tests: every repository is mocked, nothing touches a database. */
class AccountLifecycleServiceTest {

    private static final long USER_ID = 42L;
    private static final long ADMIN_ID = 7L;

    private UserRepository userRepository;
    private BookingRepository bookingRepository;
    private SettlementRepository settlementRepository;
    private NotificationRepository notificationRepository;
    private AuditLogRepository auditLogRepository;
    private AuditLogService auditLogService;
    private AccountLifecycleService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        bookingRepository = mock(BookingRepository.class);
        settlementRepository = mock(SettlementRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        auditLogRepository = mock(AuditLogRepository.class);
        auditLogService = mock(AuditLogService.class);
        service = new AccountLifecycleService(userRepository, bookingRepository, settlementRepository,
                notificationRepository, auditLogRepository, mock(AdminUserRepository.class), auditLogService);
    }

    private User user(UserRole role, boolean active, boolean deleted) {
        User user = User.builder().id(USER_ID).role(role).active(active).deleted(deleted)
                .firstName("Test").lastName("User").build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        return user;
    }

    private void lastAuditAction(String action) {
        when(auditLogRepository.findFirstByEntityTypeAndEntityIdOrderByCreatedAtDescIdDesc(
                AccountLifecycleService.ENTITY_TYPE, USER_ID))
                .thenReturn(Optional.of(AuditLog.builder().action(action).build()));
    }

    @Test
    void suspendDeactivatesAuditsAndNotifies() {
        User user = user(UserRole.VEHICLE_USER, true, false);

        assertEquals(AccountStatus.SUSPENDED, service.suspend(USER_ID, ADMIN_ID, "Abusive behaviour"));

        assertFalse(user.getActive());
        verify(auditLogService).record(AccountLifecycleService.ENTITY_TYPE, USER_ID, ADMIN_ID,
                "ACTIVE", "SUSPENDED", AccountLifecycleService.ACTION_SUSPEND, "Abusive behaviour");
        ArgumentCaptor<Notification> sent = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(sent.capture());
        assertEquals(NotificationType.ACCOUNT_SUSPENDED, sent.getValue().getNotificationType());
    }

    @Test
    void suspendRejectsAlreadySuspendedAccount() {
        user(UserRole.MECHANIC, false, false);
        assertThrows(ApiException.class, () -> service.suspend(USER_ID, ADMIN_ID, "x"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void adminAccountsCannotBeManaged() {
        user(UserRole.ADMINISTRATOR, true, false);
        assertThrows(ApiException.class, () -> service.suspend(USER_ID, ADMIN_ID, "x"));
    }

    @Test
    void restoreReactivatesPortalSuspension() {
        User user = user(UserRole.SALES_REPRESENTATIVE, false, false);
        lastAuditAction(AccountLifecycleService.ACTION_SUSPEND);

        assertEquals(AccountStatus.ACTIVE, service.restore(USER_ID, ADMIN_ID, "Appeal accepted"));
        assertTrue(user.getActive());
    }

    @Test
    void restoreRefusesAccountDeactivatedOutsidePortal() {
        user(UserRole.VEHICLE_USER, false, false);
        when(auditLogRepository.findFirstByEntityTypeAndEntityIdOrderByCreatedAtDescIdDesc(anyString(), anyLong()))
                .thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.restore(USER_ID, ADMIN_ID, "x"));
        assertEquals("DEACTIVATED_OUTSIDE_PORTAL", ex.getErrorCode());
    }

    @Test
    void restoreRefusesDeletedAccount() {
        user(UserRole.MECHANIC, false, true);
        assertThrows(ApiException.class, () -> service.restore(USER_ID, ADMIN_ID, "x"));
    }

    @Test
    void deleteIsSoftAndAlsoDeactivates() {
        User user = user(UserRole.MECHANIC, true, false);

        assertEquals(AccountStatus.DELETED, service.delete(USER_ID, ADMIN_ID, "Closed business"));

        assertTrue(user.getDeleted());
        assertFalse(user.getActive());
        assertNotNull(user.getDeletedAt());
        verify(auditLogService).record(eq(AccountLifecycleService.ENTITY_TYPE), eq(USER_ID), eq(ADMIN_ID),
                eq("ACTIVE"), eq("DELETED"), eq(AccountLifecycleService.ACTION_DELETE), eq("Closed business"));
    }

    @Test
    void deleteBlockedByOpenWorkshopBookings() {
        user(UserRole.MECHANIC, true, false);
        when(bookingRepository.countByMechanicIdAndStatusIn(eq(USER_ID), any())).thenReturn(2L);

        ApiException ex = assertThrows(ApiException.class, () -> service.delete(USER_ID, ADMIN_ID, "x"));
        assertEquals("OPEN_BOOKINGS", ex.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteBlockedByUnsettledPayouts() {
        user(UserRole.MECHANIC, false, false);
        when(settlementRepository.countByMechanicIdAndStatusIn(eq(USER_ID), any())).thenReturn(1L);

        ApiException ex = assertThrows(ApiException.class, () -> service.delete(USER_ID, ADMIN_ID, "x"));
        assertEquals("UNSETTLED_PAYMENTS", ex.getErrorCode());
    }

    @Test
    void deleteBlockedByOpenRiderBookings() {
        user(UserRole.VEHICLE_USER, true, false);
        when(bookingRepository.countByVehicleUserIdAndStatusIn(eq(USER_ID), any())).thenReturn(1L);

        assertThrows(ApiException.class, () -> service.delete(USER_ID, ADMIN_ID, "x"));
    }
}
