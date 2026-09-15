package com.alvexo.adminportal.service;

import com.alvexo.adminportal.dto.DashboardSummaryDto;
import com.alvexo.adminportal.entity.UserRole;
import com.alvexo.adminportal.entity.WorkshopVerificationStatus;
import com.alvexo.adminportal.entity.WorkshopVerificationType;
import com.alvexo.adminportal.repository.MechanicConfigurationSettingsRepository;
import com.alvexo.adminportal.repository.UserRepository;
import com.alvexo.adminportal.repository.WorkshopVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final MechanicConfigurationSettingsRepository mechanicConfigurationSettingsRepository;
    private final WorkshopVerificationRepository workshopVerificationRepository;

    public DashboardSummaryDto getSummary() {
        return DashboardSummaryDto.builder()
                .registeredRiders(userRepository.countByRoleAndDeletedFalse(UserRole.VEHICLE_USER))
                .deletedRiders(userRepository.countByRoleAndDeletedTrue(UserRole.VEHICLE_USER))
                .registeredWorkshops(userRepository.countByRoleAndDeletedFalse(UserRole.MECHANIC))
                .deletedWorkshops(userRepository.countByRoleAndDeletedTrue(UserRole.MECHANIC))
                .suspendedWorkshops(userRepository.countByRoleAndActiveFalseAndDeletedFalse(UserRole.MECHANIC))
                .listedWorkshops(workshopVerificationRepository.countByVerificationTypeAndStatus(
                        WorkshopVerificationType.LISTED, WorkshopVerificationStatus.APPROVED))
                .platformTrustedWorkshops(workshopVerificationRepository.countByVerificationTypeAndStatus(
                        WorkshopVerificationType.TRUST, WorkshopVerificationStatus.APPROVED))
                .pickupDropEnabledWorkshops(mechanicConfigurationSettingsRepository.countPickupDropEnabledForActiveMechanics())
                .advancePaymentEnabledWorkshops(mechanicConfigurationSettingsRepository.countAdvancePaymentEnabledForActiveMechanics())
                .registeredSalesAgents(userRepository.countByRoleAndDeletedFalse(UserRole.SALES_REPRESENTATIVE))
                .suspendedSalesAgents(userRepository.countByRoleAndActiveFalseAndDeletedFalse(UserRole.SALES_REPRESENTATIVE))
                .deletedSalesAgents(userRepository.countByRoleAndDeletedTrue(UserRole.SALES_REPRESENTATIVE))
                .build();
    }
}
