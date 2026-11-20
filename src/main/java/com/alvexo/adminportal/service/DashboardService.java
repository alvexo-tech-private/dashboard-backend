package com.alvexo.adminportal.service;

import com.alvexo.adminportal.dto.DashboardSummaryDto;
import com.alvexo.adminportal.entity.BookingStatus;
import com.alvexo.adminportal.entity.SettlementStatus;
import com.alvexo.adminportal.entity.SupportRequestStatus;
import com.alvexo.adminportal.entity.UserRole;
import com.alvexo.adminportal.entity.WorkshopStatus;
import com.alvexo.adminportal.repository.BookingRepository;
import com.alvexo.adminportal.repository.MechanicConfigurationSettingsRepository;
import com.alvexo.adminportal.repository.SettlementRepository;
import com.alvexo.adminportal.repository.UserRepository;
import com.alvexo.adminportal.repository.WorkshopProfileRepository;
import com.alvexo.adminportal.repository.WorkshopSupportRequestRepository;
import com.alvexo.adminportal.repository.WorkshopVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final List<BookingStatus> NOT_SCHEDULED = List.of(BookingStatus.CANCELLED, BookingStatus.REJECTED);
    private static final List<SettlementStatus> UNPAID = List.of(SettlementStatus.AWAITING_PAYOUT,
            SettlementStatus.PROCESSING, SettlementStatus.ON_HOLD, SettlementStatus.FAILED);
    private static final List<SupportRequestStatus> OPEN_SUPPORT = List.of(SupportRequestStatus.OPEN,
            SupportRequestStatus.IN_PROGRESS);

    private final UserRepository userRepository;
    private final MechanicConfigurationSettingsRepository mechanicConfigurationSettingsRepository;
    private final WorkshopProfileRepository workshopProfileRepository;
    private final WorkshopVerificationRepository workshopVerificationRepository;
    private final BookingRepository bookingRepository;
    private final SettlementRepository settlementRepository;
    private final WorkshopSupportRequestRepository workshopSupportRequestRepository;
    private final Clock clock;

    public DashboardSummaryDto getSummary() {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        return DashboardSummaryDto.builder()
                .registeredRiders(userRepository.countByRoleAndDeletedFalse(UserRole.VEHICLE_USER))
                .deletedRiders(userRepository.countByRoleAndDeletedTrue(UserRole.VEHICLE_USER))
                .registeredWorkshops(userRepository.countByRoleAndDeletedFalse(UserRole.MECHANIC))
                .deletedWorkshops(userRepository.countByRoleAndDeletedTrue(UserRole.MECHANIC))
                .suspendedWorkshops(userRepository.countByRoleAndActiveFalseAndDeletedFalse(UserRole.MECHANIC))
                // Listed = onboarding approved (Self Listed) and not paused, same as the booking API's gate.
                .listedWorkshops(workshopProfileRepository.countByStatusAndSuspendedFalse(WorkshopStatus.VERIFIED))
                .platformTrustedWorkshops(workshopVerificationRepository.countByVerifiedTrue())
                .pickupDropEnabledWorkshops(mechanicConfigurationSettingsRepository.countPickupDropEnabledForActiveMechanics())
                .advancePaymentEnabledWorkshops(mechanicConfigurationSettingsRepository.countAdvancePaymentEnabledForActiveMechanics())
                .registeredSalesAgents(userRepository.countByRoleAndDeletedFalse(UserRole.SALES_REPRESENTATIVE))
                .suspendedSalesAgents(userRepository.countByRoleAndActiveFalseAndDeletedFalse(UserRole.SALES_REPRESENTATIVE))
                .deletedSalesAgents(userRepository.countByRoleAndDeletedTrue(UserRole.SALES_REPRESENTATIVE))
                .bookingsScheduledToday(bookingRepository.countScheduledBetween(start, end, NOT_SCHEDULED))
                .bookingsCancelledToday(bookingRepository.countCancelledBetween(start, end))
                .workshopsPendingSettlement(settlementRepository.countWorkshopsWithSettlementDueBy(UNPAID, today))
                .workshopsSettledToday(settlementRepository.countWorkshopsPaidOn(SettlementStatus.PAID, today))
                .openSupportRequests(workshopSupportRequestRepository.countByStatusIn(OPEN_SUPPORT))
                .build();
    }
}
