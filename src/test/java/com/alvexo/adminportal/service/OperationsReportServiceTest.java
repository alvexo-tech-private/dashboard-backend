package com.alvexo.adminportal.service;

import com.alvexo.adminportal.dto.PagedResponse;
import com.alvexo.adminportal.dto.report.BookingReportDto;
import com.alvexo.adminportal.dto.report.RegistrationReportDto;
import com.alvexo.adminportal.dto.report.WorkshopPerformanceRowDto;
import com.alvexo.adminportal.entity.Booking;
import com.alvexo.adminportal.entity.BookingStatus;
import com.alvexo.adminportal.entity.User;
import com.alvexo.adminportal.entity.UserRole;
import com.alvexo.adminportal.report.DateRange;
import com.alvexo.adminportal.repository.BookingRepository;
import com.alvexo.adminportal.repository.ReportQueryRepository;
import com.alvexo.adminportal.repository.ReviewRepository;
import com.alvexo.adminportal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Pure unit tests: every repository is mocked, nothing touches a database. */
class OperationsReportServiceTest {

    private BookingRepository bookingRepository;
    private UserRepository userRepository;
    private ReviewRepository reviewRepository;
    private ReportQueryRepository reportQueryRepository;
    private OperationsReportService service;

    private final DateRange range = DateRange.of(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3));

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        userRepository = mock(UserRepository.class);
        reviewRepository = mock(ReviewRepository.class);
        reportQueryRepository = mock(ReportQueryRepository.class);
        service = new OperationsReportService(bookingRepository, userRepository, reviewRepository, reportQueryRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void bookingsReportListsEveryStatusAndTotalsThem() {
        when(reportQueryRepository.bookingStatusCounts(any(Specification.class))).thenReturn(List.of(
                new Object[]{BookingStatus.COMPLETED, 4L}, new Object[]{BookingStatus.CANCELLED, 1L}));
        User rider = User.builder().id(1L).firstName("Asha").lastName("Rao").build();
        User workshop = User.builder().id(2L).firstName("Ravi").lastName("K").workshopName("Ravi Motors").build();
        Booking booking = Booking.builder().id(9L).bookingNumber("BK-9").status(BookingStatus.COMPLETED)
                .vehicleUser(rider).mechanic(workshop).scheduledDateTime(LocalDateTime.of(2026, 11, 2, 10, 0)).build();
        when(bookingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(booking), PageRequest.of(0, 20), 1));

        BookingReportDto report = service.bookings(range, null, null, 0, 20);

        assertEquals(5, report.total());
        assertEquals(BookingStatus.values().length, report.statusCounts().size());
        assertEquals(0, report.statusCounts().get(0).count(), "PENDING shown as zero, not left out");
        assertEquals("Asha Rao", report.rows().getContent().get(0).riderName());
        assertEquals("Ravi Motors", report.rows().getContent().get(0).workshopName());
    }

    @Test
    void registrationsIncludeEmptyDaysNewestFirstWithTotals() {
        when(userRepository.registrationsByDayAndRole(range.start(), range.endExclusive())).thenReturn(List.of(
                new Object[]{LocalDate.of(2026, 11, 1), UserRole.VEHICLE_USER, 3L},
                new Object[]{LocalDate.of(2026, 11, 1), UserRole.MECHANIC, 1L},
                new Object[]{LocalDate.of(2026, 11, 3), UserRole.SALES_REPRESENTATIVE, 2L},
                new Object[]{LocalDate.of(2026, 11, 3), UserRole.ADMINISTRATOR, 5L}));

        RegistrationReportDto report = service.registrations(range);

        assertEquals(3, report.days().size());
        assertEquals(LocalDate.of(2026, 11, 3), report.days().get(0).date());
        assertEquals(2, report.days().get(0).salesAgents());
        assertEquals(0, report.days().get(1).total(), "a day with no sign-ups is still listed");
        assertEquals(4, report.days().get(2).total());
        assertEquals(3, report.totals().riders());
        assertEquals(6, report.totals().total(), "booking-API administrators are not counted");
        assertNull(report.totals().date());
    }

    @Test
    void workshopPerformanceComputesRatesAndJoinsRatings() {
        Page<Object[]> page = new PageImpl<>(List.<Object[]>of(
                new Object[]{2L, 8L, 6L, 1L, 1L, new BigDecimal("4200.00")},
                new Object[]{3L, 1L, 0L, 1L, 0L, null}), PageRequest.of(0, 20), 2);
        when(bookingRepository.workshopPerformance(eq(range.start()), eq(range.endExclusive()), any(Pageable.class)))
                .thenReturn(page);
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(
                User.builder().id(2L).workshopName("Ravi Motors").city("Chennai").build(),
                User.builder().id(3L).firstName("Mani").lastName("S").build()));
        when(reviewRepository.ratingsForMechanics(anyCollection(), eq(range.start()), eq(range.endExclusive())))
                .thenReturn(List.<Object[]>of(new Object[]{2L, 4.333333d, 3L}));

        PagedResponse<WorkshopPerformanceRowDto> result = service.workshopPerformance(range, 0, 20);

        WorkshopPerformanceRowDto busy = result.getContent().get(0);
        assertEquals("Ravi Motors", busy.workshopName());
        assertEquals(new BigDecimal("75.0"), busy.completionRate());
        assertEquals(new BigDecimal("4.3"), busy.averageRating());
        assertEquals(3, busy.reviews());

        WorkshopPerformanceRowDto quiet = result.getContent().get(1);
        assertEquals("Mani S", quiet.workshopName(), "falls back to the owner name");
        assertEquals(BigDecimal.ZERO, quiet.completedBookingValue());
        assertNull(quiet.averageRating());
        assertEquals(0, quiet.reviews());
    }
}
