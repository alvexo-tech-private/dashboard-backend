package com.alvexo.adminportal.service;

import com.alvexo.adminportal.dto.PagedResponse;
import com.alvexo.adminportal.dto.report.BookingReportDto;
import com.alvexo.adminportal.dto.report.BookingReportRowDto;
import com.alvexo.adminportal.dto.report.RegistrationReportDto;
import com.alvexo.adminportal.dto.report.RegistrationReportRowDto;
import com.alvexo.adminportal.dto.report.StatusCountDto;
import com.alvexo.adminportal.dto.report.WorkshopPerformanceRowDto;
import com.alvexo.adminportal.entity.Booking;
import com.alvexo.adminportal.entity.BookingStatus;
import com.alvexo.adminportal.entity.User;
import com.alvexo.adminportal.entity.UserRole;
import com.alvexo.adminportal.report.DateRange;
import com.alvexo.adminportal.report.Names;
import com.alvexo.adminportal.report.ReportColumn;
import com.alvexo.adminportal.report.ReportExportService;
import com.alvexo.adminportal.repository.BookingRepository;
import com.alvexo.adminportal.repository.ReportQueryRepository;
import com.alvexo.adminportal.repository.ReviewRepository;
import com.alvexo.adminportal.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read-only operational reports: bookings, new registrations and workshop
 * performance. Each report has one query used for both the screen and the
 * export, so an export always matches what was shown.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OperationsReportService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort BOOKING_ORDER = Sort.by(Sort.Order.desc("scheduledDateTime"), Sort.Order.desc("id"));

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ReportQueryRepository reportQueryRepository;

    // ---------------------------------------------------------------- Bookings

    public static final List<ReportColumn<BookingReportRowDto>> BOOKING_COLUMNS = List.of(
            ReportColumn.of("Booking Number", BookingReportRowDto::bookingNumber),
            ReportColumn.of("Scheduled For", BookingReportRowDto::scheduledDateTime),
            ReportColumn.of("Status", BookingReportRowDto::status),
            ReportColumn.of("Service Type", BookingReportRowDto::serviceType),
            ReportColumn.of("Booking Type", BookingReportRowDto::bookingType),
            ReportColumn.of("Channel", BookingReportRowDto::channel),
            ReportColumn.of("Rider ID", BookingReportRowDto::riderId),
            ReportColumn.of("Rider", BookingReportRowDto::riderName),
            ReportColumn.of("Workshop ID", BookingReportRowDto::workshopId),
            ReportColumn.of("Workshop", BookingReportRowDto::workshopName),
            ReportColumn.of("Estimated Cost (INR)", BookingReportRowDto::estimatedCost),
            ReportColumn.of("Actual Cost (INR)", BookingReportRowDto::actualCost),
            ReportColumn.of("Advance Paid (INR)", BookingReportRowDto::advancePaid),
            ReportColumn.of("Completed At", BookingReportRowDto::completedAt),
            ReportColumn.of("Cancelled At", BookingReportRowDto::cancelledAt),
            ReportColumn.of("Cancellation Reason", BookingReportRowDto::cancellationReason));

    public BookingReportDto bookings(DateRange range, BookingStatus status, Long workshopId, int page, int size) {
        Specification<Booking> filter = bookingFilter(range, status, workshopId);

        Map<BookingStatus, Long> counts = new EnumMap<>(BookingStatus.class);
        for (Object[] row : reportQueryRepository.bookingStatusCounts(filter)) {
            counts.put((BookingStatus) row[0], (Long) row[1]);
        }
        List<StatusCountDto> statusCounts = new ArrayList<>();
        long total = 0;
        for (BookingStatus s : BookingStatus.values()) {
            long count = counts.getOrDefault(s, 0L);
            statusCounts.add(new StatusCountDto(s.name(), count));
            total += count;
        }

        Page<Booking> rows = bookingRepository.findAll(filter, PageRequest.of(Math.max(page, 0), clamp(size), BOOKING_ORDER));
        return new BookingReportDto(total, statusCounts, PagedResponse.from(rows, OperationsReportService::toRow));
    }

    /** Every matching row for an export (capped at {@link ReportExportService#fetchLimit()}). */
    public List<BookingReportRowDto> allBookings(DateRange range, BookingStatus status, Long workshopId) {
        return bookingRepository.findAll(bookingFilter(range, status, workshopId),
                        PageRequest.of(0, ReportExportService.fetchLimit(), BOOKING_ORDER))
                .map(OperationsReportService::toRow).getContent();
    }

    static Specification<Booking> bookingFilter(DateRange range, BookingStatus status, Long workshopId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.greaterThanOrEqualTo(root.get("scheduledDateTime"), range.start()));
            predicates.add(cb.lessThan(root.get("scheduledDateTime"), range.endExclusive()));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (workshopId != null) {
                predicates.add(cb.equal(root.get("mechanic").get("id"), workshopId));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    static BookingReportRowDto toRow(Booking b) {
        return new BookingReportRowDto(
                b.getId(), b.getBookingNumber(), b.getScheduledDateTime(), name(b.getStatus()),
                name(b.getServiceType()), name(b.getBookingType()), name(b.getChannel()),
                b.getVehicleUser() == null ? null : b.getVehicleUser().getId(), Names.person(b.getVehicleUser()),
                b.getMechanic() == null ? null : b.getMechanic().getId(), Names.workshop(b.getMechanic()),
                b.getEstimatedCost(), b.getActualCost(), b.getAdvancePaid(),
                b.getCompletedAt(), b.getCancelledAt(), b.getCancellationReason());
    }

    // ----------------------------------------------------------- Registrations

    public static final List<ReportColumn<RegistrationReportRowDto>> REGISTRATION_COLUMNS = List.of(
            ReportColumn.of("Date", RegistrationReportRowDto::date),
            ReportColumn.of("Riders", RegistrationReportRowDto::riders),
            ReportColumn.of("Workshops", RegistrationReportRowDto::workshops),
            ReportColumn.of("Sales Agents", RegistrationReportRowDto::salesAgents),
            ReportColumn.of("Total", RegistrationReportRowDto::total));

    /** One row per day in the range, newest first, including days with no sign-ups. */
    public RegistrationReportDto registrations(DateRange range) {
        Map<LocalDate, Map<UserRole, Long>> byDay = new HashMap<>();
        for (Object[] row : userRepository.registrationsByDayAndRole(range.start(), range.endExclusive())) {
            LocalDate day = toLocalDate(row[0]);
            byDay.computeIfAbsent(day, d -> new EnumMap<>(UserRole.class)).merge((UserRole) row[1], (Long) row[2], Long::sum);
        }

        List<RegistrationReportRowDto> days = new ArrayList<>();
        long riders = 0, workshops = 0, agents = 0;
        for (LocalDate day = range.to(); !day.isBefore(range.from()); day = day.minusDays(1)) {
            Map<UserRole, Long> counts = byDay.getOrDefault(day, Map.of());
            long r = counts.getOrDefault(UserRole.VEHICLE_USER, 0L);
            long w = counts.getOrDefault(UserRole.MECHANIC, 0L);
            long a = counts.getOrDefault(UserRole.SALES_REPRESENTATIVE, 0L);
            days.add(new RegistrationReportRowDto(day, r, w, a, r + w + a));
            riders += r;
            workshops += w;
            agents += a;
        }
        RegistrationReportRowDto totals = new RegistrationReportRowDto(null, riders, workshops, agents, riders + workshops + agents);
        return new RegistrationReportDto(totals, days);
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        if (value instanceof java.time.LocalDateTime dateTime) {
            return dateTime.toLocalDate();
        }
        throw new IllegalStateException("Unexpected date value: " + value);
    }

    // ---------------------------------------------------- Workshop performance

    public static final List<ReportColumn<WorkshopPerformanceRowDto>> WORKSHOP_PERFORMANCE_COLUMNS = List.of(
            ReportColumn.of("Workshop ID", WorkshopPerformanceRowDto::workshopId),
            ReportColumn.of("Workshop", WorkshopPerformanceRowDto::workshopName),
            ReportColumn.of("City", WorkshopPerformanceRowDto::city),
            ReportColumn.of("Total Bookings", WorkshopPerformanceRowDto::totalBookings),
            ReportColumn.of("Completed", WorkshopPerformanceRowDto::completed),
            ReportColumn.of("Cancelled", WorkshopPerformanceRowDto::cancelled),
            ReportColumn.of("Rejected", WorkshopPerformanceRowDto::rejected),
            ReportColumn.of("Completion Rate (%)", WorkshopPerformanceRowDto::completionRate),
            ReportColumn.of("Completed Booking Value (INR)", WorkshopPerformanceRowDto::completedBookingValue),
            ReportColumn.of("Average Rating", WorkshopPerformanceRowDto::averageRating),
            ReportColumn.of("Reviews", WorkshopPerformanceRowDto::reviews));

    /** Workshops with at least one booking scheduled in the range, busiest first. */
    public PagedResponse<WorkshopPerformanceRowDto> workshopPerformance(DateRange range, int page, int size) {
        Page<Object[]> rows = bookingRepository.workshopPerformance(range.start(), range.endExclusive(),
                PageRequest.of(Math.max(page, 0), clamp(size)));
        return PagedResponse.from(rows, performanceMapper(range, rows.getContent()));
    }

    public List<WorkshopPerformanceRowDto> allWorkshopPerformance(DateRange range) {
        List<Object[]> rows = bookingRepository.workshopPerformance(range.start(), range.endExclusive(),
                PageRequest.of(0, ReportExportService.fetchLimit())).getContent();
        return rows.stream().map(performanceMapper(range, rows)).toList();
    }

    private Function<Object[], WorkshopPerformanceRowDto> performanceMapper(DateRange range, List<Object[]> rows) {
        List<Long> ids = rows.stream().map(r -> (Long) r[0]).toList();
        if (ids.isEmpty()) {
            return r -> null;
        }
        Map<Long, User> workshops = userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, Object[]> ratings = reviewRepository.ratingsForMechanics(ids, range.start(), range.endExclusive())
                .stream().collect(Collectors.toMap(r -> (Long) r[0], Function.identity()));

        return r -> {
            Long id = (Long) r[0];
            long total = number(r[1]);
            long completed = number(r[2]);
            User workshop = workshops.get(id);
            Object[] rating = ratings.get(id);
            return new WorkshopPerformanceRowDto(
                    id, Names.workshop(workshop), workshop == null ? null : workshop.getCity(),
                    total, completed, number(r[3]), number(r[4]),
                    total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(completed * 100.0 / total).setScale(1, RoundingMode.HALF_UP),
                    r[5] == null ? BigDecimal.ZERO : (BigDecimal) r[5],
                    rating == null || rating[1] == null ? null
                            : BigDecimal.valueOf(((Number) rating[1]).doubleValue()).setScale(1, RoundingMode.HALF_UP),
                    rating == null ? 0 : number(rating[2]));
        };
    }

    // ----------------------------------------------------------------- helpers

    private static long number(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }

    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    static int clamp(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }
}
