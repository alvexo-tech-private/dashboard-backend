package com.alvexo.adminportal.service;

import com.alvexo.adminportal.dto.DashboardSummaryDto;
import com.alvexo.adminportal.entity.UserRole;
import com.alvexo.adminportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;

    public DashboardSummaryDto getSummary() {
        return DashboardSummaryDto.builder()
                .registeredRiders(userRepository.countByRoleAndDeletedFalse(UserRole.VEHICLE_USER))
                .deletedRiders(userRepository.countByRoleAndDeletedTrue(UserRole.VEHICLE_USER))
                .registeredWorkshops(userRepository.countByRoleAndDeletedFalse(UserRole.MECHANIC))
                .deletedWorkshops(userRepository.countByRoleAndDeletedTrue(UserRole.MECHANIC))
                .build();
    }
}
