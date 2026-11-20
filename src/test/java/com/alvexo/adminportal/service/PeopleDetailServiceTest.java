package com.alvexo.adminportal.service;

import com.alvexo.adminportal.dto.BookingRowDto;
import com.alvexo.adminportal.dto.PagedResponse;
import com.alvexo.adminportal.dto.ReferralRowDto;
import com.alvexo.adminportal.dto.RiderDetailDto;
import com.alvexo.adminportal.entity.Booking;
import com.alvexo.adminportal.entity.BookingStatus;
import com.alvexo.adminportal.entity.Referral;
import com.alvexo.adminportal.entity.User;
import com.alvexo.adminportal.entity.UserRole;
import com.alvexo.adminportal.entity.Vehicle;
import com.alvexo.adminportal.exception.ApiException;
import com.alvexo.adminportal.repository.BookingRepository;
import com.alvexo.adminportal.repository.ReferralRepository;
import com.alvexo.adminportal.repository.ReviewRepository;
import com.alvexo.adminportal.repository.UserRepository;
import com.alvexo.adminportal.repository.UserVehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pure unit tests: every repository is mocked, nothing touches a database. */
class PeopleDetailServiceTest {

    private UserRepository userRepository;
    private BookingRepository bookingRepository;
    private UserVehicleRepository userVehicleRepository;
    private ReferralRepository referralRepository;
    private PeopleDetailService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        bookingRepository = mock(BookingRepository.class);
        userVehicleRepository = mock(UserVehicleRepository.class);
        referralRepository = mock(ReferralRepository.class);
        service = new PeopleDetailService(userRepository, bookingRepository, userVehicleRepository,
                mock(ReviewRepository.class), referralRepository);
    }

    private User user(long id, UserRole role) {
        User user = User.builder().id(id).role(role).active(true).deleted(false)
                .firstName("Asha").lastName("Rao").workshopName(role == UserRole.MECHANIC ? "Rao Motors" : null)
                .build();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void riderBookingsUseTheRiderSideAndShowBothParties() {
        User rider = user(1L, UserRole.VEHICLE_USER);
        User mechanic = User.builder().id(2L).role(UserRole.MECHANIC).firstName("Ravi").lastName("K")
                .workshopName("Ravi Bikes").build();
        Booking booking = Booking.builder().id(10L).bookingNumber("BK-1").vehicleUser(rider).mechanic(mechanic)
                .vehicle(Vehicle.builder().make("Honda").model("Shine").year(2022).build())
                .status(BookingStatus.COMPLETED).scheduledDateTime(LocalDateTime.now()).build();
        Page<Booking> page = new PageImpl<>(List.of(booking));
        when(bookingRepository.findByVehicleUserId(eq(1L), any(Pageable.class))).thenReturn(page);

        PagedResponse<BookingRowDto> result = service.getBookings(1L, 0, 10, "ALL");

        BookingRowDto row = result.getContent().get(0);
        assertEquals("Ravi Bikes", row.getWorkshopName());
        assertEquals("Asha Rao", row.getRiderName());
        assertEquals("Honda Shine (2022)", row.getVehicle());
        verify(bookingRepository, never()).findByMechanicId(anyLong(), any());
    }

    @Test
    void workshopBookingsFilterByStatus() {
        user(2L, UserRole.MECHANIC);
        when(bookingRepository.findByMechanicIdAndStatus(eq(2L), eq(BookingStatus.CANCELLED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getBookings(2L, 0, 10, "cancelled");

        verify(bookingRepository).findByMechanicIdAndStatus(eq(2L), eq(BookingStatus.CANCELLED), any(Pageable.class));
    }

    @Test
    void unknownBookingStatusIsABadRequest() {
        user(2L, UserRole.MECHANIC);
        assertThrows(ApiException.class, () -> service.getBookings(2L, 0, 10, "LOST"));
    }

    @Test
    void riderDetailRejectsOtherRoles() {
        user(3L, UserRole.SALES_REPRESENTATIVE);
        assertThrows(ApiException.class, () -> service.getRider(3L));
    }

    @Test
    void riderDetailStillShownAfterDeletion() {
        User rider = user(1L, UserRole.VEHICLE_USER);
        rider.setDeleted(true);
        rider.setActive(false);
        when(userVehicleRepository.findByUserIdOrderByIsPrimaryDescCreatedAtAsc(1L)).thenReturn(List.of());
        when(bookingRepository.findWorkshopsUsedByRider(eq(1L), any())).thenReturn(List.of());

        RiderDetailDto detail = service.getRider(1L);

        assertEquals("DELETED", detail.getAccountStatus());
    }

    @Test
    void referralRowsNameTheWorkshopOnlyForMechanics() {
        user(5L, UserRole.SALES_REPRESENTATIVE);
        User workshop = User.builder().id(6L).role(UserRole.MECHANIC).active(true).deleted(false)
                .firstName("Ravi").lastName("K").workshopName("Ravi Bikes").build();
        User rider = User.builder().id(7L).role(UserRole.VEHICLE_USER).active(false).deleted(false)
                .firstName("Meena").lastName("S").build();
        when(referralRepository.findBySalesRepId(eq(5L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(
                Referral.builder().id(1L).referredUser(workshop).build(),
                Referral.builder().id(2L).referredUser(rider).build())));

        List<ReferralRowDto> rows = service.getReferrals(5L, 0, 10).getContent();

        assertEquals("Ravi Bikes", rows.get(0).getWorkshopName());
        assertNull(rows.get(1).getWorkshopName());
        assertEquals("SUSPENDED", rows.get(1).getReferredAccountStatus());
    }
}
