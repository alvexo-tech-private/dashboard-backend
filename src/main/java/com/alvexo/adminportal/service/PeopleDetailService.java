package com.alvexo.adminportal.service;

import com.alvexo.adminportal.dto.BookingCountsDto;
import com.alvexo.adminportal.dto.BookingRowDto;
import com.alvexo.adminportal.dto.PagedResponse;
import com.alvexo.adminportal.dto.ReferralRowDto;
import com.alvexo.adminportal.dto.ReviewRowDto;
import com.alvexo.adminportal.dto.RiderDetailDto;
import com.alvexo.adminportal.dto.RiderVehicleDto;
import com.alvexo.adminportal.dto.RiderWorkshopDto;
import com.alvexo.adminportal.dto.SalesAgentDetailDto;
import com.alvexo.adminportal.entity.AccountStatus;
import com.alvexo.adminportal.entity.Booking;
import com.alvexo.adminportal.entity.BookingStatus;
import com.alvexo.adminportal.entity.Referral;
import com.alvexo.adminportal.entity.Review;
import com.alvexo.adminportal.entity.User;
import com.alvexo.adminportal.entity.UserRole;
import com.alvexo.adminportal.entity.UserVehicle;
import com.alvexo.adminportal.entity.Vehicle;
import com.alvexo.adminportal.exception.ApiException;
import com.alvexo.adminportal.repository.BookingRepository;
import com.alvexo.adminportal.repository.ReferralRepository;
import com.alvexo.adminportal.repository.ReviewRepository;
import com.alvexo.adminportal.repository.UserRepository;
import com.alvexo.adminportal.repository.UserVehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Read-only detail views behind the people directories:
 * Rider Details (ADMIN-US-08 BR-07) and Rider History (ADMIN-US-09),
 * Sales Agent details and referred workshops (ADMIN-US-10 BR-09/10), and the
 * booking / review history shared by the rider and workshop pages.
 *
 * Everything is derived from the booking API's own tables (no copies), and
 * every record stays visible after an account is suspended or deleted
 * (ADMIN-US-08 BR-11, ADMIN-US-09 quality DoD).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PeopleDetailService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int WORKSHOPS_SHOWN = 10;
    private static final List<BookingStatus> UPCOMING_STATUSES = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final UserVehicleRepository userVehicleRepository;
    private final ReviewRepository reviewRepository;
    private final ReferralRepository referralRepository;

    public RiderDetailDto getRider(Long id) {
        User rider = getUser(id, UserRole.VEHICLE_USER, "Rider not found.");

        BookingCountsDto bookings = BookingCountsDto.builder()
                .total(bookingRepository.countByVehicleUserId(id))
                .upcoming(bookingRepository.countUpcomingByVehicleUserId(id, UPCOMING_STATUSES, LocalDateTime.now()))
                .completed(bookingRepository.countByVehicleUserIdAndStatus(id, BookingStatus.COMPLETED))
                .cancelled(bookingRepository.countByVehicleUserIdAndStatus(id, BookingStatus.CANCELLED))
                .build();

        List<RiderVehicleDto> vehicles = userVehicleRepository.findByUserIdOrderByIsPrimaryDescCreatedAtAsc(id).stream()
                .map(this::toVehicleDto)
                .toList();

        List<RiderWorkshopDto> workshops = bookingRepository
                .findWorkshopsUsedByRider(id, PageRequest.of(0, WORKSHOPS_SHOWN)).stream()
                .map(row -> RiderWorkshopDto.builder()
                        .mechanicId((Long) row[0])
                        .workshopName((String) row[1])
                        .bookings((Long) row[2])
                        .lastBookingAt((LocalDateTime) row[3])
                        .build())
                .toList();

        return RiderDetailDto.builder()
                .id(rider.getId())
                .firstName(rider.getFirstName())
                .lastName(rider.getLastName())
                .email(rider.getEmail())
                .mobileNumber(rider.getMobileNumber())
                .emailVerified(rider.getEmailVerified())
                .mobileVerified(rider.getMobileVerified())
                .addressLine1(rider.getAddressLine1())
                .addressLine2(rider.getAddressLine2())
                .area(rider.getArea())
                .city(rider.getCity())
                .state(rider.getState())
                .postalCode(rider.getPostalCode())
                .registeredAt(rider.getCreatedAt())
                .accountStatus(AccountStatus.of(rider).name())
                .deletedAt(rider.getDeletedAt())
                .bookings(bookings)
                .vehicles(vehicles)
                .workshops(workshops)
                .build();
    }

    public SalesAgentDetailDto getSalesAgent(Long id) {
        User agent = getUser(id, UserRole.SALES_REPRESENTATIVE, "Sales agent not found.");
        return SalesAgentDetailDto.builder()
                .id(agent.getId())
                .firstName(agent.getFirstName())
                .lastName(agent.getLastName())
                .email(agent.getEmail())
                .mobileNumber(agent.getMobileNumber())
                .city(agent.getCity())
                .state(agent.getState())
                .referralCode(agent.getReferralCode())
                .registeredAt(agent.getCreatedAt())
                .accountStatus(AccountStatus.of(agent).name())
                .deletedAt(agent.getDeletedAt())
                .referredWorkshops(referralRepository.countBySalesRepIdAndReferredUserRole(id, UserRole.MECHANIC))
                .activeReferredWorkshops(referralRepository
                        .countBySalesRepIdAndReferredUserRoleAndReferredUserActiveTrueAndReferredUserDeletedFalse(
                                id, UserRole.MECHANIC))
                .referredOthers(referralRepository.countBySalesRepIdAndReferredUserRoleNot(id, UserRole.MECHANIC))
                .bonusPaid(referralRepository.sumBonus(id, true))
                .bonusPending(referralRepository.sumBonus(id, false))
                .build();
    }

    /** Booking history of a rider or a workshop, newest scheduled first, optionally one status only. */
    public PagedResponse<BookingRowDto> getBookings(Long userId, int page, int size, String status) {
        User user = getRiderOrWorkshop(userId);
        BookingStatus filter = parseBookingStatus(status);
        Pageable pageable = pageable(page, size, Sort.by(Sort.Order.desc("scheduledDateTime"), Sort.Order.desc("id")));

        Page<Booking> result;
        if (user.getRole() == UserRole.VEHICLE_USER) {
            result = filter == null
                    ? bookingRepository.findByVehicleUserId(userId, pageable)
                    : bookingRepository.findByVehicleUserIdAndStatus(userId, filter, pageable);
        } else {
            result = filter == null
                    ? bookingRepository.findByMechanicId(userId, pageable)
                    : bookingRepository.findByMechanicIdAndStatus(userId, filter, pageable);
        }
        return PagedResponse.from(result, this::toBookingRow);
    }

    /** Reviews a rider wrote, or reviews a workshop received; newest first. */
    public PagedResponse<ReviewRowDto> getReviews(Long userId, int page, int size) {
        User user = getRiderOrWorkshop(userId);
        Pageable pageable = pageable(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<Review> result = user.getRole() == UserRole.VEHICLE_USER
                ? reviewRepository.findByVehicleUserId(userId, pageable)
                : reviewRepository.findByMechanicId(userId, pageable);
        return PagedResponse.from(result, this::toReviewRow);
    }

    /** Accounts a sales agent referred, newest first. */
    public PagedResponse<ReferralRowDto> getReferrals(Long salesAgentId, int page, int size) {
        getUser(salesAgentId, UserRole.SALES_REPRESENTATIVE, "Sales agent not found.");
        Pageable pageable = pageable(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return PagedResponse.from(referralRepository.findBySalesRepId(salesAgentId, pageable), this::toReferralRow);
    }

    // ---- helpers ----

    private User getUser(Long id, UserRole role, String notFoundMessage) {
        return userRepository.findById(id)
                .filter(u -> u.getRole() == role)
                .orElseThrow(() -> ApiException.notFound(notFoundMessage));
    }

    private User getRiderOrWorkshop(Long id) {
        return userRepository.findById(id)
                .filter(u -> u.getRole() == UserRole.VEHICLE_USER || u.getRole() == UserRole.MECHANIC)
                .orElseThrow(() -> ApiException.notFound("Rider or workshop not found."));
    }

    private Pageable pageable(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), sort);
    }

    /** Blank / "ALL" means no filter; anything else must be a BookingStatus. */
    private BookingStatus parseBookingStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status.trim())) {
            return null;
        }
        try {
            return BookingStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Unknown booking status: " + status);
        }
    }

    private static String fullName(User user) {
        if (user == null) return null;
        String name = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return name.isEmpty() ? null : name;
    }

    private static String workshopName(User mechanic) {
        if (mechanic == null) return null;
        return mechanic.getWorkshopName() != null && !mechanic.getWorkshopName().isBlank()
                ? mechanic.getWorkshopName()
                : fullName(mechanic);
    }

    private static String vehicleLabel(Vehicle vehicle) {
        if (vehicle == null) return null;
        String label = String.join(" ", List.of(
                vehicle.getMake() == null ? "" : vehicle.getMake(),
                vehicle.getModel() == null ? "" : vehicle.getModel())).trim();
        return vehicle.getYear() != null ? label + " (" + vehicle.getYear() + ")" : label;
    }

    private RiderVehicleDto toVehicleDto(UserVehicle uv) {
        Vehicle v = uv.getVehicle();
        return RiderVehicleDto.builder()
                .id(uv.getId())
                .make(v != null ? v.getMake() : null)
                .model(v != null ? v.getModel() : null)
                .year(uv.getRegistrationYear() != null ? uv.getRegistrationYear() : (v != null ? v.getYear() : null))
                .vehicleType(v != null && v.getVehicleType() != null ? v.getVehicleType().name() : null)
                .registrationNumber(uv.getRegistrationNumber())
                .primary(Boolean.TRUE.equals(uv.getIsPrimary()))
                .addedAt(uv.getCreatedAt())
                .build();
    }

    private BookingRowDto toBookingRow(Booking b) {
        return BookingRowDto.builder()
                .id(b.getId())
                .bookingNumber(b.getBookingNumber())
                .riderId(b.getVehicleUser() != null ? b.getVehicleUser().getId() : null)
                .riderName(fullName(b.getVehicleUser()))
                .mechanicId(b.getMechanic() != null ? b.getMechanic().getId() : null)
                .workshopName(workshopName(b.getMechanic()))
                .vehicle(vehicleLabel(b.getVehicle()))
                .serviceType(b.getServiceType() != null ? b.getServiceType().name() : null)
                .status(b.getStatus() != null ? b.getStatus().name() : null)
                .scheduledAt(b.getScheduledDateTime())
                .bookedAt(b.getCreatedAt())
                .estimatedCost(b.getEstimatedCost())
                .actualCost(b.getActualCost())
                .cancelledAt(b.getCancelledAt())
                .cancellationReason(b.getCancellationReason())
                .build();
    }

    private ReviewRowDto toReviewRow(Review r) {
        return ReviewRowDto.builder()
                .id(r.getId())
                .bookingNumber(r.getBooking() != null ? r.getBooking().getBookingNumber() : null)
                .riderId(r.getVehicleUser() != null ? r.getVehicleUser().getId() : null)
                .riderName(fullName(r.getVehicleUser()))
                .mechanicId(r.getMechanic() != null ? r.getMechanic().getId() : null)
                .workshopName(workshopName(r.getMechanic()))
                .rating(r.getRating())
                .comment(r.getComment())
                .workshopResponse(r.getResponse())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private ReferralRowDto toReferralRow(Referral r) {
        User referred = r.getReferredUser();
        return ReferralRowDto.builder()
                .id(r.getId())
                .referredUserId(referred != null ? referred.getId() : null)
                .referredName(fullName(referred))
                .workshopName(referred != null && referred.getRole() == UserRole.MECHANIC ? workshopName(referred) : null)
                .referredRole(referred != null && referred.getRole() != null ? referred.getRole().name() : null)
                .referredAccountStatus(referred != null ? AccountStatus.of(referred).name() : null)
                .status(r.getStatus() != null ? r.getStatus().name() : null)
                .bonusAmount(r.getBonusAmount())
                .bonusPaid(Boolean.TRUE.equals(r.getBonusPaid()))
                .referredAt(r.getCreatedAt())
                .build();
    }
}
