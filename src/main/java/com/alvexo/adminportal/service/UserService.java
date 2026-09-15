package com.alvexo.adminportal.service;

import com.alvexo.adminportal.dto.MechanicSummaryDto;
import com.alvexo.adminportal.dto.PagedResponse;
import com.alvexo.adminportal.dto.SalesAgentSummaryDto;
import com.alvexo.adminportal.dto.VehicleUserSummaryDto;
import com.alvexo.adminportal.dto.WorkshopSummaryDto;
import com.alvexo.adminportal.entity.BookingStatus;
import com.alvexo.adminportal.entity.MechanicConfigurationSettings;
import com.alvexo.adminportal.entity.Settlement;
import com.alvexo.adminportal.entity.User;
import com.alvexo.adminportal.entity.UserRole;
import com.alvexo.adminportal.exception.ApiException;
import com.alvexo.adminportal.repository.BookingRepository;
import com.alvexo.adminportal.repository.MechanicConfigurationSettingsRepository;
import com.alvexo.adminportal.repository.ReferralRepository;
import com.alvexo.adminportal.repository.SettlementRepository;
import com.alvexo.adminportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> MECHANIC_SORT_FIELDS = Set.of(
            "firstName", "lastName", "email", "workshopName", "city", "state",
            "specialization", "experienceYears", "hourlyRate", "rating", "active", "createdAt"
    );

    private static final Set<String> VEHICLE_USER_SORT_FIELDS = Set.of(
            "firstName", "lastName", "email", "city", "state",
            "emailVerified", "mobileVerified", "active", "createdAt"
    );

    private static final Set<String> SALES_AGENT_SORT_FIELDS = Set.of(
            "firstName", "lastName", "email", "city", "referralCode",
            "totalReferrals", "totalBonusEarned", "active", "createdAt"
    );

    private static final List<BookingStatus> UPCOMING_BOOKING_STATUSES = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    private final UserRepository userRepository;
    private final MechanicConfigurationSettingsRepository mechanicConfigurationSettingsRepository;
    private final ReferralRepository referralRepository;
    private final BookingRepository bookingRepository;
    private final SettlementRepository settlementRepository;

    public PagedResponse<MechanicSummaryDto> getMechanics(int page, int size, String search, String sortBy, String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir, MECHANIC_SORT_FIELDS);
        Page<User> result = userRepository.searchMechanics(UserRole.MECHANIC, normalize(search), pageable);

        List<Long> mechanicIds = result.getContent().stream().map(User::getId).toList();
        Map<Long, MechanicConfigurationSettings> settingsByMechanicId = mechanicConfigurationSettingsRepository
                .findByMechanicIdIn(mechanicIds).stream()
                .collect(Collectors.toMap(s -> s.getMechanic().getId(), Function.identity()));
        Map<Long, String> salesAgentNameByMechanicId = latestReferralByReferredUserId(mechanicIds);

        return PagedResponse.from(result, user -> toMechanicSummaryDto(user, settingsByMechanicId, salesAgentNameByMechanicId));
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

    /**
     * ADMIN-US-02 Workshop Summary, BR-06 through BR-11. See WorkshopSummaryDto
     * for what is intentionally left out and why.
     */
    public WorkshopSummaryDto getMechanicSummary(Long id) {
        User mechanic = userRepository.findById(id)
                .filter(u -> u.getRole() == UserRole.MECHANIC)
                .orElseThrow(() -> ApiException.notFound("Workshop not found."));

        MechanicConfigurationSettings settings = mechanicConfigurationSettingsRepository.findByMechanicId(id).orElse(null);
        String salesAgentName = referralRepository.findFirstByReferredUserIdOrderByCreatedAtDesc(id)
                .map(referral -> fullName(referral.getSalesRep()))
                .orElse(null);

        long totalBookings = bookingRepository.countByMechanicId(id);
        long upcomingBookings = bookingRepository.countUpcomingByMechanicId(id, UPCOMING_BOOKING_STATUSES, LocalDateTime.now());
        long cancelledBookings = bookingRepository.countByMechanicIdAndStatus(id, BookingStatus.CANCELLED);

        Settlement lastSettlement = settlementRepository.findFirstByMechanicIdOrderBySettlementDateDesc(id).orElse(null);

        return WorkshopSummaryDto.builder()
                .id(mechanic.getId())
                .workshopName(mechanic.getWorkshopName())
                .ownerName(fullName(mechanic))
                .mobileNumber(mechanic.getMobileNumber())
                .addressLine1(mechanic.getAddressLine1())
                .addressLine2(mechanic.getAddressLine2())
                .city(mechanic.getCity())
                .state(mechanic.getState())
                .postalCode(mechanic.getPostalCode())
                .country(mechanic.getCountry())
                .registeredAt(mechanic.getCreatedAt())
                .pickupDropEnabled(settings != null && Boolean.TRUE.equals(settings.getPickupDropFacilityEnabled()))
                .advancePaymentEnabled(settings != null && Boolean.TRUE.equals(settings.getRepairsRequireAdvance()))
                .salesAgentName(salesAgentName)
                .accountStatus(accountStatus(mechanic))
                .totalBookings(totalBookings)
                .upcomingBookings(upcomingBookings)
                .cancelledBookings(cancelledBookings)
                .lastSettlementDate(lastSettlement != null ? lastSettlement.getSettlementDate() : null)
                .lastSettlementStatus(lastSettlement != null ? lastSettlement.getStatus() : null)
                .build();
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

    private String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    private String accountStatus(User user) {
        if (Boolean.TRUE.equals(user.getDeleted())) return "DELETED";
        if (!Boolean.TRUE.equals(user.getActive())) return "SUSPENDED";
        return "ACTIVE";
    }

    /** Collapses to one referral per referred user, keeping the most recent if a user was referred more than once. */
    private Map<Long, String> latestReferralByReferredUserId(List<Long> mechanicIds) {
        Map<Long, String> result = new HashMap<>();
        for (var referral : referralRepository.findByReferredUserIdIn(mechanicIds)) {
            result.putIfAbsent(referral.getReferredUser().getId(), fullName(referral.getSalesRep()));
        }
        return result;
    }

    private MechanicSummaryDto toMechanicSummaryDto(User user, Map<Long, MechanicConfigurationSettings> settingsByMechanicId,
                                                      Map<Long, String> salesAgentNameByMechanicId) {
        MechanicConfigurationSettings settings = settingsByMechanicId.get(user.getId());
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
                .registeredAt(user.getCreatedAt())
                .pickupDropEnabled(settings != null && Boolean.TRUE.equals(settings.getPickupDropFacilityEnabled()))
                .advancePaymentEnabled(settings != null && Boolean.TRUE.equals(settings.getRepairsRequireAdvance()))
                .accountStatus(accountStatus(user))
                .salesAgentName(salesAgentNameByMechanicId.get(user.getId()))
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
