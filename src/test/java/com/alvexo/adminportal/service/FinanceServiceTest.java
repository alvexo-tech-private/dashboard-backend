package com.alvexo.adminportal.service;

import com.alvexo.adminportal.dto.FinanceDtos.BookingFinance;
import com.alvexo.adminportal.dto.FinanceDtos.FinanceDashboard;
import com.alvexo.adminportal.dto.FinanceDtos.OfflineClosureRequest;
import com.alvexo.adminportal.entity.*;
import com.alvexo.adminportal.exception.ApiException;
import com.alvexo.adminportal.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** ADMIN-EPIC-06: read-only finance views and the Super Admin offline closure. */
class FinanceServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 20);

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final RefundRepository refundRepository = mock(RefundRepository.class);
    private final PayoutBatchRepository batchRepository = mock(PayoutBatchRepository.class);
    private final SettlementRepository settlementRepository = mock(SettlementRepository.class);
    private final BookingRepository bookingRepository = mock(BookingRepository.class);
    private final UserVehicleRepository userVehicleRepository = mock(UserVehicleRepository.class);
    private final FinanceAuditLogRepository financeAuditRepository = mock(FinanceAuditLogRepository.class);
    private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    private final AdminUserRepository adminUserRepository = mock(AdminUserRepository.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final Clock clock = Clock.fixed(DAY.plusDays(3).atTime(11, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant(),
            ZoneId.of("Asia/Kolkata"));

    private FinanceService service;
    private User workshop;
    private User rider;
    private Booking booking;
    private Settlement settlement;
    private PayoutBatch batch;

    @BeforeEach
    void setUp() {
        service = new FinanceService(paymentRepository, refundRepository, batchRepository, settlementRepository,
                bookingRepository, userVehicleRepository, financeAuditRepository, auditLogRepository, adminUserRepository,
                auditLogService, notificationRepository, clock);
        workshop = User.builder().id(3L).workshopName("Test Workshop 2").area("BTM").build();
        rider = User.builder().id(2L).firstName("Test").lastName("Rider").mobileNumber("9000000001").build();
        booking = Booking.builder().id(17L).bookingNumber("1888-TN-0017").mechanic(workshop).vehicleUser(rider)
                .status(BookingStatus.COMPLETED).scheduledDateTime(DAY.atTime(10, 30)).build();
        batch = PayoutBatch.builder().id(1L).batchReference("WPB-20261120-WS003").mechanic(workshop).batchDate(DAY)
                .status(PayoutBatchStatus.TO_BE_PAID).totalAmount(new BigDecimal("29.70")).bookingCount(1).build();
        settlement = Settlement.builder().id(117L).mechanic(workshop).booking(booking).settlementDate(DAY)
                .advanceCollected(new BigDecimal("40")).advanceHandlingFee(BigDecimal.TEN).netPay(new BigDecimal("30"))
                .tcsRate(new BigDecimal("0.01")).tcsDeduction(new BigDecimal("0.30")).finalPayout(new BigDecimal("29.70"))
                .status(SettlementStatus.AWAITING_PAYOUT).batch(batch).build();
        when(settlementRepository.findByBatch(batch)).thenReturn(List.of(settlement));
        when(batchRepository.findByBatchDate(DAY)).thenReturn(List.of(batch));
        when(batchRepository.findByReference("WPB-20261120-WS003")).thenReturn(Optional.of(batch));
        when(batchRepository.findByReferenceForUpdate("WPB-20261120-WS003")).thenReturn(Optional.of(batch));
        when(paymentRepository.findBookingPayments(any(), any())).thenReturn(List.of());
        when(userVehicleRepository.findFirstByUserIdAndVehicleId(any(), any())).thenReturn(Optional.empty());
    }

    private Payment payment(Booking b, String advance) {
        return Payment.builder().booking(b).amount(new BigDecimal(advance).add(BigDecimal.TEN))
                .advanceAmount(new BigDecimal(advance)).platformFeeAmount(BigDecimal.TEN).status(PaymentStatus.COMPLETED)
                .transactionId("BKG-" + b.getId() + "-1").gatewayPaymentId("CF-" + b.getId())
                .paymentDate(DAY.atTime(9, 0)).build();
    }

    @Test
    void dashboardShowsCollectionsRefundsAndBatchCountsForTheDate() {
        when(paymentRepository.findCapturedBetween(any(), eq(DAY.atStartOfDay()), eq(DAY.plusDays(1).atStartOfDay())))
                .thenReturn(List.of(payment(booking, "40"), payment(booking, "100")));
        when(refundRepository.findCompletedBetween(any(), any())).thenReturn(List.of(
                Refund.builder().totalAmount(new BigDecimal("40")).status(RefundStatus.COMPLETED).build()));

        FinanceDashboard d = service.dashboard(DAY);

        assertEquals(0, new BigDecimal("140").compareTo(d.collection().advanceTotal()));
        assertEquals(0, new BigDecimal("20").compareTo(d.collection().platformFeeTotal()));
        assertEquals(2, d.collection().transactionCount());
        assertEquals(1, d.refunds().count());
        assertEquals(1, d.batches().pending());
        var row = d.workshopSettlements().get(0);
        assertEquals("WS-003", row.workshopCode());
        assertEquals("PENDING", row.status());
        assertEquals(0, new BigDecimal("10.30").compareTo(row.platformCharges()));
        assertEquals(0, new BigDecimal("29.70").compareTo(row.netSettlement()));
    }

    @Test
    void statusLabelsFollowTheSpec() {
        assertEquals("PENDING", FinanceService.label(PayoutBatchStatus.TO_BE_PAID));
        assertEquals("COMPLETED", FinanceService.label(PayoutBatchStatus.PAID));
        assertEquals("EXCEPTION", FinanceService.label(PayoutBatchStatus.PARTIALLY_PAID));
        assertEquals("FAILED", FinanceService.label(PayoutBatchStatus.FAILED));
    }

    @Test
    void bookingBreakdownOfACancelledRefundedBooking() {
        Booking cancelled = Booking.builder().id(20L).bookingNumber("1888-TN-0019").mechanic(workshop).vehicleUser(rider)
                .status(BookingStatus.CANCELLED).scheduledDateTime(DAY.atTime(10, 30)).build();
        Payment p = payment(cancelled, "40");
        when(bookingRepository.findByBookingNumberWithParties("1888-TN-0019")).thenReturn(List.of(cancelled));
        when(paymentRepository.findBookingPayments(List.of(20L), FinanceService.CAPTURED)).thenReturn(List.of(p));
        when(settlementRepository.findByBookingId(20L)).thenReturn(Optional.empty());
        when(refundRepository.findByBookingIdOrderByIdAsc(20L)).thenReturn(List.of(Refund.builder().reason("RIDER_CANCEL")
                .totalAmount(new BigDecimal("40")).status(RefundStatus.COMPLETED).providerRefundId("cf_ref").build()));

        BookingFinance f = service.booking("1888-TN-0019");

        assertEquals("CF-20", f.payment().cashfreePaymentId());
        assertEquals(0, BigDecimal.ZERO.compareTo(f.charges().workshopShare()));
        assertEquals(0, BigDecimal.TEN.compareTo(f.charges().bykCommission()));
        assertEquals("REFUNDED", f.refunds().get(0).status());
        assertEquals("cf_ref", f.refunds().get(0).cashfreeRefundId());
        assertNull(f.exception());
        assertThrows(ApiException.class, () -> service.booking("NOPE"));
    }

    @Test
    void offlineClosurePaysTheBookingsNotifiesTheWorkshopAndAudits() {
        when(batchRepository.lockSequence(DAY.plusDays(3))).thenReturn(4);

        service.closeOffline("WPB-20261120-WS003",
                new OfflineClosureRequest("UTR-123", "Paid by NEFT, gateway payouts not live yet", null), 1L);

        assertEquals(SettlementStatus.PAID, settlement.getStatus());
        assertEquals("WPO-20261123-000005", settlement.getPayoutReference());
        assertEquals("UTR-123", settlement.getBankReference());
        assertEquals(DAY.plusDays(3), settlement.getPayoutDate());
        assertEquals(PayoutBatchStatus.PAID, batch.getStatus());
        assertEquals("UTR-123", batch.getUtr());
        verify(batchRepository).updateSequence(DAY.plusDays(3), 5);
        ArgumentCaptor<Notification> n = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(n.capture());
        assertEquals(NotificationType.SETTLEMENT_PAID, n.getValue().getNotificationType());
        verify(auditLogService).record(eq("PAYOUT_BATCH"), eq(1L), eq(1L), eq("TO_BE_PAID"), eq("PAID"),
                eq("OFFLINE_CLOSURE"), contains("UTR-123"));
    }

    @Test
    void offlineClosureRefusesACompletedBatchAndMissingDetails() {
        batch.setStatus(PayoutBatchStatus.PAID);
        ApiException e = assertThrows(ApiException.class, () -> service.closeOffline("WPB-20261120-WS003",
                new OfflineClosureRequest("UTR-1", "x", null), 1L));
        assertEquals("BATCH_ALREADY_CLOSED", e.getErrorCode());

        batch.setStatus(PayoutBatchStatus.TO_BE_PAID);
        ApiException missing = assertThrows(ApiException.class, () -> service.closeOffline("WPB-20261120-WS003",
                new OfflineClosureRequest(" ", "x", null), 1L));
        assertEquals("CLOSURE_DETAILS_REQUIRED", missing.getErrorCode());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void overviewCardsCompareWithThePreviousPeriodAndListTransactionsWithStatus() {
        LocalDate today = DAY.plusDays(3);
        Payment paid = payment(booking, "40");
        paid.setUser(rider);
        Booking refundedBooking = Booking.builder().id(20L).bookingNumber("1888-TN-0019").mechanic(workshop)
                .vehicleUser(rider).status(BookingStatus.CANCELLED).build();
        Payment refundedPayment = payment(refundedBooking, "40");
        refundedPayment.setUser(rider);
        when(paymentRepository.findCapturedBetween(any(), eq(today.minusDays(29).atStartOfDay()), any()))
                .thenReturn(List.of(paid, refundedPayment));
        when(paymentRepository.findCapturedBetween(any(), eq(today.minusDays(59).atStartOfDay()), any()))
                .thenReturn(List.of(payment(booking, "70")));
        when(settlementRepository.findBySettlementDateBetween(any(), any())).thenReturn(List.of());
        settlement.setStatus(SettlementStatus.PAID);
        when(settlementRepository.findPaidBetween(any(), any())).thenReturn(List.of());
        when(refundRepository.findCompletedBetween(any(), any())).thenReturn(List.of());
        when(settlementRepository.findByBookingIds(any())).thenReturn(List.of(settlement));
        when(refundRepository.findByBookingIds(any())).thenReturn(List.of(Refund.builder().booking(refundedBooking)
                .status(RefundStatus.COMPLETED).totalAmount(new BigDecimal("40")).providerRefundId("cf_ref_77").build()));

        var o = service.overview(null, null);

        assertEquals(today.minusDays(29), o.from());
        assertEquals(0, new BigDecimal("100").compareTo(o.customerPayments().amount()));
        assertEquals(0, new BigDecimal("80").compareTo(o.customerPayments().previousAmount()));
        assertEquals(0, new BigDecimal("25.0").compareTo(o.customerPayments().changePercent()));
        assertEquals(30, o.customerPayments().trend().size());
        assertNull(o.refunds().changePercent());
        assertEquals("PAID", o.recentTransactions().get(0).status());
        assertEquals("REFUNDED", o.recentTransactions().get(1).status());
        assertEquals("Test Rider", o.recentTransactions().get(0).customerName());
        // Cashfree ids on the row: payment id, and the refund id once refunded
        assertEquals("CF-17", o.recentTransactions().get(0).cashfreePaymentId());
        assertNull(o.recentTransactions().get(0).cashfreeRefundId());
        assertEquals("CF-20", o.recentTransactions().get(1).cashfreePaymentId());
        assertEquals("cf_ref_77", o.recentTransactions().get(1).cashfreeRefundId());
        // the Booking ID filter also finds a payment by its Cashfree payment / refund id
        assertEquals(1, service.transactions(null, null, null, null, "CF-20", null, null, null, 0, 20).total());
        assertEquals(1, service.transactions(null, null, null, null, "cf_ref_77", null, null, null, 0, 20).total());

        var page = service.transactions(null, null, "0019", null, null, null, null, null, 0, 20);
        assertEquals(1, page.total());
        assertEquals("1888-TN-0019", page.rows().get(0).bookingNumber());

        // filters: Booking ID, customer name, payment date (that day only)
        assertEquals(1, service.transactions(null, null, null, null, "tn-0017", null, null, null, 0, 20).total());
        assertEquals(2, service.transactions(null, null, null, null, null, "test rid", null, null, 0, 20).total());
        assertEquals(0, service.transactions(null, null, null, null, null, "nobody", null, null, 0, 20).total());
        // workshop filter: by name or by ID (WS-3 / WS-003)
        assertEquals(2, service.transactions(null, null, null, null, null, null, "test work", null, 0, 20).total());
        assertEquals(2, service.transactions(null, null, null, null, null, null, "WS-003", null, 0, 20).total());
        assertEquals(0, service.transactions(null, null, null, null, null, null, "other garage", null, 0, 20).total());
        when(paymentRepository.findCapturedBetween(any(), eq(DAY.atStartOfDay()), eq(DAY.plusDays(1).atStartOfDay())))
                .thenReturn(List.of(paid));
        var onDay = service.transactions(null, null, null, null, null, null, null, DAY, 0, 20);
        assertEquals(1, onDay.total());
        assertEquals("1888-TN-0017", onDay.rows().get(0).bookingNumber());
    }

    @Test
    void zeroAdvanceBookingsAreListedAsNoPayment() {
        LocalDate today = DAY.plusDays(3);
        when(paymentRepository.findCapturedBetween(any(), any(), any())).thenReturn(List.of());
        Booking sameDay = Booking.builder().id(18L).bookingNumber("1888-TN-0018").mechanic(workshop).vehicleUser(rider)
                .status(BookingStatus.COMPLETED).channel(BookingChannel.RIDER_APP).pickupRequired(false)
                .createdAt(DAY.atTime(13, 26)).build();
        Booking walkIn = Booking.builder().id(30L).bookingNumber("1888-TN-0030").mechanic(workshop).vehicleUser(rider)
                .status(BookingStatus.CONFIRMED).channel(BookingChannel.WALK_IN).createdAt(today.atTime(9, 0)).build();
        when(bookingRepository.findWithoutPaymentBetween(any(), any(), any(), any())).thenReturn(List.of(sameDay, walkIn));

        var page = service.transactions(null, null, null, "NO_PAYMENT", null, null, null, null, 0, 20);

        assertEquals(2, page.total());
        assertEquals("1888-TN-0030", page.rows().get(0).bookingNumber());   // newest first
        assertEquals("WALK_IN", page.rows().get(0).detailedStatus());
        assertEquals("SAME_DAY", page.rows().get(1).detailedStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(page.rows().get(1).amount()));
        assertNull(page.rows().get(1).cashfreePaymentId());
        // the cards only count real Cashfree money
        when(settlementRepository.findBySettlementDateBetween(any(), any())).thenReturn(List.of());
        when(settlementRepository.findPaidBetween(any(), any())).thenReturn(List.of());
        when(refundRepository.findCompletedBetween(any(), any())).thenReturn(List.of());
        var o = service.overview(null, null);
        assertEquals(0, BigDecimal.ZERO.compareTo(o.customerPayments().amount()));
        assertEquals(2, o.recentTransactions().size());
    }
}
