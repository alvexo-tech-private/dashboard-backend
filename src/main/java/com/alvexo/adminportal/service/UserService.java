package com.alvexo.adminportal.service;

import com.alvexo.adminportal.dto.MechanicSummaryDto;
import com.alvexo.adminportal.dto.PagedResponse;
import com.alvexo.adminportal.dto.SalesAgentSummaryDto;
import com.alvexo.adminportal.dto.VehicleUserSummaryDto;
import com.alvexo.adminportal.entity.User;
import com.alvexo.adminportal.entity.UserRole;
import com.alvexo.adminportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> MECHANIC_SORT_FIELDS = Set.of(
            "firstName", "lastName", "email", "workshopName", "city", "state",
            "specialization", "experienceYears", "hourlyRate", "rating", "active"
    );

    private static final Set<String> VEHICLE_USER_SORT_FIELDS = Set.of(
            "firstName", "lastName", "email", "city", "state",
            "emailVerified", "mobileVerified", "active", "createdAt"
    );

    private static final Set<String> SALES_AGENT_SORT_FIELDS = Set.of(
            "firstName", "lastName", "email", "city", "referralCode",
            "totalReferrals", "totalBonusEarned", "active", "createdAt"
    );

    private final UserRepository userRepository;

    public PagedResponse<MechanicSummaryDto> getMechanics(int page, int size, String search, String sortBy, String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir, MECHANIC_SORT_FIELDS);
        Page<User> result = userRepository.searchMechanics(UserRole.MECHANIC, normalize(search), pageable);
        return PagedResponse.from(result, this::toMechanicSummaryDto);
    }

    public PagedResponse<VehicleUserSummaryDto> getVehicleUsers(int page, int size, String search, String sortBy, String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir, VEHICLE_USER_SORT_FIELDS);
        Page<User> result = userRepository.searchVehicleUsers(UserRole.VEHICLE_USER, normalize(search), pageable);
        return PagedResponse.from(result, this::toVehicleUserSummaryDto);
    }

    public PagedResponse<SalesAgentSummaryDto> getSalesAgents(int page, int size, String search, String sortBy, String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir, SALES_AGENT_SORT_FIELDS);
        Page<User> result = userRepository.searchSalesAgents(UserRole.SALES_REPRESENTATIVE, normalize(search), pageable);
        return PagedResponse.from(result, this::toSalesAgentSummaryDto);
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir, Set<String> allowedSortFields) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String sortField = allowedSortFields.contains(sortBy) ? sortBy : "id";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, sortField));
    }

    private String normalize(String search) {
        return search == null ? "" : search.trim();
    }

    private MechanicSummaryDto toMechanicSummaryDto(User user) {
        return MechanicSummaryDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .mobileNumber(user.getMobileNumber())
                .workshopName(user.getWorkshopName())
                .city(user.getCity())
                .state(user.getState())
                .specialization(user.getSpecialization())
                .experienceYears(user.getExperienceYears())
                .hourlyRate(user.getHourlyRate())
                .rating(user.getRating())
                .active(user.getActive())
                .build();
    }

    private VehicleUserSummaryDto toVehicleUserSummaryDto(User user) {
        return VehicleUserSummaryDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .mobileNumber(user.getMobileNumber())
                .city(user.getCity())
                .state(user.getState())
                .emailVerified(user.getEmailVerified())
                .mobileVerified(user.getMobileVerified())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private SalesAgentSummaryDto toSalesAgentSummaryDto(User user) {
        return SalesAgentSummaryDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .mobileNumber(user.getMobileNumber())
                .city(user.getCity())
                .referralCode(user.getReferralCode())
                .totalReferrals(user.getTotalReferrals())
                .totalBonusEarned(user.getTotalBonusEarned())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
