package com.alvexo.adminportal.service;

import com.alvexo.adminportal.dto.FinanceDtos.*;
import com.alvexo.adminportal.entity.*;
import com.alvexo.adminportal.exception.ApiException;
import org.springframework.http.HttpStatus;
import com.alvexo.adminportal.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Finance module (ADMIN-EPIC-06). The portal monitors money moved by the platform backend and Cashfree: it reads
 * payments, refunds, settlements and the day-wise payout batches, and never calculates or alters gateway amounts
 * (US-11 BR-10/28). Refunds are executed automatically by Cashfree under the app's cancellation rules — there is
 * no refund action here (BR-25–27). The one write is the Super Admin offline closure of a batch paid outside the
 * gateway, with mandatory UTR + comment, audited.
 */
@Service
@RequiredArgsConstructor
public class FinanceService {

    static final Set<PaymentStatus> CAPTURED = EnumSet.of(
            PaymentStatus.COMPLETED, PaymentStatus.REFUNDED, PaymentStatus.PARTIALLY_REFUNDED);
    private static final Set<PayoutBatchStatus> CLOSABLE = EnumSet.of(PayoutBatchStatus.TO_BE_PAID,
            PayoutBatchStatus.PROCESSING, PayoutBatchStatus.FAILED, PayoutBatchStatus.PARTIALLY_PAID);
    private static final Set<SettlementStatus> UNPAID = EnumSet.of(
            SettlementStatus.AWAITING_PAYOUT, SettlementStatus.PROCESSING, SettlementStatus.FAILED);
    private static final DateTimeFormatter REF_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    static final String AUDIT_ENTITY = "PAYOUT_BATCH";

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PayoutBatchRepository batchRepository;
    private final SettlementRepository settlementRepository;
    private final BookingRepository bookingRepository;
    private final UserVehicleRepository userVehicleRepository;
    private final FinanceAuditLogRepository financeAuditRepository;
    private final AuditLogRepository auditLogRepository;
    private final AdminUserRepository adminUserRepository;
    private final AuditLogService auditLogService;
    private final NotificationRepository notificationRepository;
    private final Clock clock;

    // ── US-11 Finance Dashboard ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public FinanceDashboard dashboard(LocalDate date) {
        LocalDate day = date != null ? date : LocalDate.now(clock);
        List<Payment> payments = paymentRepository.findCapturedBetween(CAPTURED, day.atStartOfDay(),
                day.plusDays(1).atStartOfDay());
        BigDecimal advance = sum(payments.stream().map(this::advanceOf));
        BigDecimal fees = sum(payments.stream().map(p -> scale(p.getPlatformFeeAmount())));
        List<Refund> refunds = refundRepository.findCompletedBetween(day.atStartOfDay(), day.plusDays(1).atStartOfDay());

        List<SettlementBatchRow> rows = batchRepository.findByBatchDate(day).stream().map(this::row).toList();
        Map<String, Long> byStatus = rows.stream().collect(Collectors.groupingBy(SettlementBatchRow::status,
                Collectors.counting()));
        BatchCounts counts = new BatchCounts(rows.size(), count(byStatus, "PENDING"), count(byStatus, "PROCESSING"),
                count(byStatus, "COMPLETED"), count(byStatus, "FAILED"), count(byStatus, "EXCEPTION"));

        return new FinanceDashboard(day,
                new CollectionSummary(advance, fees, advance.add(fees), payments.size()),
                new RefundSummary(sum(refunds.stream().map(r -> scale(r.getTotalAmount()))), refunds.size()),
                counts, rows);
    }

    // ── Finance Dashboard (period cards + recent transactions) ───────────────

    private static final Set<SettlementStatus> PAYABLE = EnumSet.of(SettlementStatus.AWAITING_PAYOUT,
            SettlementStatus.PROCESSING, SettlementStatus.FAILED, SettlementStatus.ON_HOLD);
    private static final int MAX_DAYS = 366;
    /** Bookings that really exist (unpaid attempts, requests and expired ones are not bookings). */
    private static final Set<BookingStatus> REAL_BOOKING = EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.SCHEDULED,
            BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED, BookingStatus.CANCELLED);

    /**
     * The four cards for [from, to] (default: last 30 days ending today) — customer payments (gross, by payment date),
     * workshop payable (unpaid settlements, by settlement date), settled (paid settlements, by payout date) and
     * refunds (completed, by refund date) — each with the change vs the previous period of equal length and a
     * daily trend; plus the 5 newest transactions.
     */
    @Transactional(readOnly = true)
    public FinanceOverview overview(LocalDate from, LocalDate to) {
        LocalDate[] r = range(from, to);
        long days = java.time.temporal.ChronoUnit.DAYS.between(r[0], r[1]) + 1;
        LocalDate prevTo = r[0].minusDays(1);
        LocalDate prevFrom = prevTo.minusDays(days - 1);

        List<Payment> payments = paymentRepository.findCapturedBetween(CAPTURED, r[0].atStartOfDay(), r[1].plusDays(1).atStartOfDay());
        List<Payment> prevPayments = paymentRepository.findCapturedBetween(CAPTURED, prevFrom.atStartOfDay(), r[0].atStartOfDay());
        List<Settlement> payable = settlementRepository.findBySettlementDateBetween(r[0], r[1]).stream()
                .filter(s -> PAYABLE.contains(s.getStatus())).toList();
        List<Settlement> prevPayable = settlementRepository.findBySettlementDateBetween(prevFrom, prevTo).stream()
                .filter(s -> PAYABLE.contains(s.getStatus())).toList();
        List<Settlement> settled = settlementRepository.findPaidBetween(r[0], r[1]);
        List<Settlement> prevSettled = settlementRepository.findPaidBetween(prevFrom, prevTo);
        List<Refund> refunds = refundRepository.findCompletedBetween(r[0].atStartOfDay(), r[1].plusDays(1).atStartOfDay());
        List<Refund> prevRefunds = refundRepository.findCompletedBetween(prevFrom.atStartOfDay(), r[0].atStartOfDay());

        return new FinanceOverview(r[0], r[1],
                card(payments, prevPayments, p -> p.getPaymentDate().toLocalDate(), p -> scale(p.getAmount()), r),
                card(payable, prevPayable, Settlement::getSettlementDate, s -> scale(s.getFinalPayout()), r),
                card(settled, prevSettled, Settlement::getPayoutDate, s -> scale(s.getFinalPayout()), r),
                card(refunds, prevRefunds, x -> x.getCompletedAt().toLocalDate(), x -> scale(x.getTotalAmount()), r),
                withNoPaymentRows(rows(payments), r[0], r[1]).stream().limit(5).toList(), LocalDateTime.now(clock));
    }

    /**
     * Rider payments, newest first. Filters (combinable): q = Booking ID / Cashfree order or payment ID / name;
     * bookingId = BYK Booking ID or a Cashfree order / payment / refund id (partial); customer = rider name (partial); workshop = workshop name or ID (WS-3 / WS-003 / 3,
     * partial); paymentDate = that day only (overrides
     * the period); status = main or detailed status.
     */
    @Transactional(readOnly = true)
    public TransactionPage transactions(LocalDate from, LocalDate to, String q, String status, String bookingId,
                                        String customer, String workshop, LocalDate paymentDate, int page, int size) {
        LocalDate[] r = paymentDate != null ? new LocalDate[]{paymentDate, paymentDate} : range(from, to);
        int pageSize = Math.min(Math.max(size, 1), 100);
        int pageNo = Math.max(page, 0);
        List<TransactionRow> all = withNoPaymentRows(rows(paymentRepository.findCapturedBetween(CAPTURED,
                r[0].atStartOfDay(), r[1].plusDays(1).atStartOfDay())), r[0], r[1]).stream()
                .filter(t -> blank(status) || t.status().equalsIgnoreCase(status.trim())
                        || t.detailedStatus().equalsIgnoreCase(status.trim()))
                .filter(t -> blank(q) || contains(q, t.bookingNumber(), String.valueOf(t.bookingId()), t.orderId(),
                        t.cashfreePaymentId(), t.customerName(), t.workshopName()))
                .filter(t -> blank(bookingId) || contains(bookingId, t.bookingNumber(), String.valueOf(t.bookingId()),
                        t.orderId(), t.cashfreePaymentId(), t.cashfreeRefundId()))
                .filter(t -> blank(customer) || contains(customer, t.customerName()))
                .filter(t -> blank(workshop) || contains(workshop, t.workshopName(), code(t.workshopId()),
                        "WS-" + t.workshopId(), String.valueOf(t.workshopId())))
                .toList();
        int start = Math.min(pageNo * pageSize, all.size());
        return new TransactionPage(all.subList(start, Math.min(start + pageSize, all.size())), pageNo, pageSize, all.size());
    }

    private List<TransactionRow> rows(List<Payment> payments) {
        List<Long> ids = payments.stream().map(p -> p.getBooking().getId()).distinct().toList();
        Map<Long, Settlement> settlements = ids.isEmpty() ? Map.of() : settlementRepository.findByBookingIds(ids).stream()
                .collect(Collectors.toMap(s -> s.getBooking().getId(), Function.identity(), (a, b) -> a));
        Map<Long, List<Refund>> refunds = ids.isEmpty() ? Map.of() : refundRepository.findByBookingIds(ids).stream()
                .collect(Collectors.groupingBy(x -> x.getBooking().getId()));
        return payments.stream().map(p -> {
            Booking b = p.getBooking();
            List<Refund> bookingRefunds = refunds.getOrDefault(b.getId(), List.of());
            String[] st = status(b, p, settlements.get(b.getId()), bookingRefunds);
            // Cashfree refund id: the completed refund's, else the latest one already sent to Cashfree.
            String refundId = bookingRefunds.stream().filter(r -> r.getStatus() == RefundStatus.COMPLETED)
                    .map(Refund::getProviderRefundId).filter(Objects::nonNull).reduce((a, c) -> c)
                    .orElse(bookingRefunds.stream().map(Refund::getProviderRefundId).filter(Objects::nonNull)
                            .reduce((a, c) -> c).orElse(null));
            return new TransactionRow(b.getId(), b.getBookingNumber(), name(p.getUser()), b.getMechanic().getId(),
                    b.getMechanic().getWorkshopName(), p.getPaymentDate(), scale(p.getAmount()), st[0], st[1],
                    p.getTransactionId(), p.getGatewayPaymentId(), refundId);
        }).toList();
    }

    /**
     * Adds the bookings with no Cashfree payment (₹0 — same-day, pickup, walk-in) as NO_PAYMENT rows, dated by when the
     * booking was made, and returns everything newest first.
     */
    private List<TransactionRow> withNoPaymentRows(List<TransactionRow> paid, LocalDate from, LocalDate to) {
        List<TransactionRow> all = new ArrayList<>(paid);
        for (Booking b : bookingRepository.findWithoutPaymentBetween(from.atStartOfDay(), to.plusDays(1).atStartOfDay(),
                REAL_BOOKING, CAPTURED)) {
            String reason = b.getStatus() == BookingStatus.CANCELLED ? "CANCELLED"
                    : b.getChannel() == BookingChannel.WALK_IN ? "WALK_IN"
                    : Boolean.TRUE.equals(b.getPickupRequired()) ? "PICKUP" : "SAME_DAY";
            all.add(new TransactionRow(b.getId(), b.getBookingNumber(), name(b.getVehicleUser()), b.getMechanic().getId(),
                    b.getMechanic().getWorkshopName(), b.getCreatedAt(), ZERO, "NO_PAYMENT", reason, null, null, null));
        }
        all.sort(Comparator.comparing(TransactionRow::paymentDate, Comparator.nullsLast(Comparator.reverseOrder())));
        return all;
    }

    /** [main status, detailed status]. */
    String[] status(Booking b, Payment p, Settlement s, List<Refund> refunds) {
        if (refunds.stream().anyMatch(r -> r.getStatus() == RefundStatus.COMPLETED)) {
            return new String[]{"REFUNDED", p.getStatus() == PaymentStatus.REFUNDED ? "REFUNDED" : "PARTIALLY_REFUNDED"};
        }
        if (!refunds.isEmpty()) {
            boolean failed = refunds.stream().anyMatch(r -> r.getStatus() == RefundStatus.FAILED);
            return new String[]{"REFUND_PROCESSING", failed ? "REFUND_FAILED" : "REFUND_PROCESSING"};
        }
        if (b.getStatus() == BookingStatus.CANCELLED) {
            return new String[]{"CANCELLED", "NOT_REFUNDABLE"};
        }
        if (s == null) {
            return new String[]{"TO_BE_PAID", "NOT_COMPLETED"};
        }
        return switch (s.getStatus()) {
            case PAID -> new String[]{"PAID", "PAID"};
            case NO_SETTLEMENT_REQUIRED -> new String[]{"PAID", "NO_PAYOUT"};
            case AWAITING_PAYOUT -> new String[]{"TO_BE_PAID", s.getExpectedSettlementDate() != null
                    && s.getExpectedSettlementDate().isAfter(LocalDate.now(clock)) ? "IN_HOLDING" : "TO_BE_PAID"};
            default -> new String[]{"TO_BE_PAID", s.getStatus().name()};
        };
    }

    private <T> OverviewCard card(List<T> now, List<T> before, Function<T, LocalDate> dateOf,
                                  Function<T, BigDecimal> amountOf, LocalDate[] r) {
        BigDecimal amount = sum(now.stream().map(amountOf));
        BigDecimal previous = sum(before.stream().map(amountOf));
        BigDecimal change = previous.signum() == 0 ? null
                : amount.subtract(previous).multiply(BigDecimal.valueOf(100)).divide(previous, 1, RoundingMode.HALF_UP);
        Map<LocalDate, BigDecimal> byDay = new HashMap<>();
        for (T item : now) {
            LocalDate d = dateOf.apply(item);
            if (d != null) byDay.merge(d, amountOf.apply(item), BigDecimal::add);
        }
        List<BigDecimal> trend = new ArrayList<>();
        for (LocalDate d = r[0]; !d.isAfter(r[1]); d = d.plusDays(1)) {
            trend.add(byDay.getOrDefault(d, ZERO));
        }
        return new OverviewCard(amount, now.size(), previous, change, trend);
    }

    private LocalDate[] range(LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now(clock);
        LocalDate start = from != null ? from : end.minusDays(29);
        if (start.isAfter(end)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "From date must be on or before To date");
        }
        if (java.time.temporal.ChronoUnit.DAYS.between(start, end) >= MAX_DAYS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "Choose a period of at most " + MAX_DAYS + " days");
        }
        return new LocalDate[]{start, end};
    }

    // ── US-13 Workshop Settlement Summary ────────────────────────────────────

    /** Workshop-wise batches for the date; optional status label (PENDING…EXCEPTION) and workshop name/ID search. */
    @Transactional(readOnly = true)
    public List<SettlementBatchRow> batches(LocalDate date, String status, String search) {
        LocalDate day = date != null ? date : LocalDate.now(clock);
        return batchRepository.findByBatchDate(day).stream().map(this::row)
                .filter(r -> blank(status) || r.status().equalsIgnoreCase(status.trim()))
                .filter(r -> blank(search) || contains(search, r.workshopName(), r.workshopCode(),
                        String.valueOf(r.workshopId()), r.batchReference()))
                .toList();
    }

    @Transactional(readOnly = true)
    public SettlementBatchDetail batch(String reference) {
        PayoutBatch batch = batchRepository.findByReference(reference)
                .orElseThrow(() -> ApiException.notFound("Settlement batch " + reference + " not found"));
        List<Settlement> members = settlementRepository.findByBatch(batch);
        List<Long> ids = members.stream().map(s -> s.getBooking().getId()).toList();
        Map<Long, Payment> payments = ids.isEmpty() ? Map.of() : paymentRepository.findBookingPayments(ids, CAPTURED)
                .stream().collect(Collectors.toMap(p -> p.getBooking().getId(), Function.identity(), (a, b) -> a));

        List<BatchBooking> bookings = members.stream().map(s -> {
            Booking b = s.getBooking();
            Payment p = payments.get(b.getId());
            BigDecimal charges = scale(s.getAdvanceHandlingFee()).add(scale(s.getTcsDeduction()));
            return new BatchBooking(b.getId(), b.getBookingNumber(), name(b.getVehicleUser()), vehicleNumber(b),
                    b.getScheduledDateTime() == null ? null : b.getScheduledDateTime().toLocalDate(),
                    scale(s.getAdvanceCollected()), charges, scale(s.getFinalPayout()), s.getStatus().name(),
                    p == null ? null : p.getStatus().name(), p == null ? null : p.getTransactionId(),
                    p == null ? null : p.getGatewayPaymentId(), s.getPayoutReference());
        }).toList();

        List<HistoryEntry> history = new ArrayList<>();
        financeAuditRepository.findByEntityTypeAndEntityReferenceOrderByCreatedAtAscIdAsc("BATCH", reference)
                .forEach(a -> history.add(new HistoryEntry(a.getCreatedAt(), a.getAction(),
                        join(a.getFromStatus() == null ? null : a.getFromStatus() + " → " + a.getToStatus(),
                                a.getAmount() == null ? null : "₹" + scale(a.getAmount()), a.getReference(), a.getNote()),
                        "System")));
        auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(AUDIT_ENTITY, batch.getId())
                .forEach(a -> history.add(new HistoryEntry(a.getCreatedAt(), a.getAction(),
                        join(a.getPreviousStatus() + " → " + a.getNewStatus(), a.getReason()),
                        adminUserRepository.findById(a.getPerformedByAdminId()).map(AdminUser::getFullName).orElse("Admin"))));
        history.sort(Comparator.comparing(HistoryEntry::at, Comparator.nullsLast(Comparator.naturalOrder())));

        SettlementBatchRow row = row(batch);
        return new SettlementBatchDetail(row, batch.getNote(), batch.getFailureReason(),
                "FAILED".equals(row.status()) || "EXCEPTION".equals(row.status()), bookings, history);
    }

    // ── US-12 Booking Financial Breakdown ────────────────────────────────────

    @Transactional(readOnly = true)
    public BookingFinance booking(String bookingRef) {
        Booking b = findBooking(bookingRef);
        Payment p = paymentRepository.findBookingPayments(List.of(b.getId()), CAPTURED).stream().findFirst().orElse(null);
        Settlement s = settlementRepository.findByBookingId(b.getId()).orElse(null);
        List<Refund> refunds = refundRepository.findByBookingIdOrderByIdAsc(b.getId());

        User rider = b.getVehicleUser();
        User w = b.getMechanic();
        PaymentDetail payment = p == null ? null : new PaymentDetail(p.getTransactionId(), p.getGatewayPaymentId(),
                p.getPaymentDate(), p.getPaymentMethod(), advanceOf(p), scale(p.getPlatformFeeAmount()), scale(p.getAmount()),
                p.getStatus().name());

        Charges charges = null;
        if (p != null) {
            BigDecimal fee = scale(p.getPlatformFeeAmount());
            if (s != null) {
                charges = new Charges(scale(s.getAdvanceHandlingFee()), scale(s.getTcsDeduction()), scale(s.getFinalPayout()),
                        fee.add(scale(s.getAdvanceHandlingFee())), p.getGatewayCharges());
            } else if (b.getStatus() == BookingStatus.CANCELLED) {
                BigDecimal refunded = sum(refunds.stream().filter(r -> r.getStatus() != RefundStatus.FAILED)
                        .map(r -> scale(r.getTotalAmount())));
                charges = new Charges(ZERO, ZERO, ZERO, scale(p.getAmount()).subtract(refunded).max(ZERO), p.getGatewayCharges());
            } else {
                charges = new Charges(null, null, null, fee, p.getGatewayCharges()); // settled after the service
            }
        }

        List<RefundDetail> refundDetails = refunds.stream().map(r -> new RefundDetail(r.getReason(),
                r.getStatus() == RefundStatus.COMPLETED ? "REFUNDED" : r.getStatus() == RefundStatus.FAILED ? "FAILED" : "PROCESSING",
                scale(r.getTotalAmount()), r.getCreatedAt(), r.getCompletedAt(), r.getProviderRefundId(), r.getLastError()))
                .toList();

        BookingSettlement settlement = s == null ? null : new BookingSettlement(s.getStatus().name(),
                s.getBatch() == null ? null : s.getBatch().getBatchReference(),
                s.getBatch() == null ? null : label(s.getBatch().getStatus()), s.getSettlementDate(),
                scale(s.getFinalPayout()), s.getPayoutReference(), s.getBankReference(), s.getExpectedSettlementDate(),
                s.getHoldReason());

        String exception = null;
        if (refunds.stream().anyMatch(r -> r.getStatus() == RefundStatus.FAILED)) {
            exception = "Refund failed at the gateway";
        } else if (s != null && s.getStatus() == SettlementStatus.FAILED) {
            exception = "Workshop settlement failed";
        } else if (s != null && s.getStatus() == SettlementStatus.ON_HOLD) {
            exception = "Workshop settlement on hold" + (s.getHoldReason() != null ? ": " + s.getHoldReason() : "");
        } else if (s != null && s.getBatch() != null && s.getBatch().getStatus() == PayoutBatchStatus.PARTIALLY_PAID) {
            exception = "Settlement batch only partly paid";
        }

        return new BookingFinance(b.getId(), b.getBookingNumber(), b.getStatus().name(),
                b.getScheduledDateTime() == null ? null : b.getScheduledDateTime().toLocalDate(),
                new Party(rider.getId(), null, name(rider), rider.getMobileNumber(), null),
                new Party(w.getId(), code(w.getId()), w.getWorkshopName(), w.getMobileNumber(), w.getArea()),
                payment, charges, refundDetails, settlement, exception);
    }

    // ── Super Admin offline closure ──────────────────────────────────────────

    /**
     * A batch paid outside the gateway (bank transfer) is closed by a Super Admin with the bank UTR and a mandatory
     * comment: its unpaid bookings become PAID with a WPO payout reference, the UTR and today's date (the workshop's
     * Finance tab shows Paid and the workshop is notified), the batch becomes PAID, and the action is audited.
     */
    @Transactional
    public SettlementBatchDetail closeOffline(String reference, OfflineClosureRequest request, Long adminId) {
        String utr = trim(request.utr());
        String comment = trim(request.comment());
        if (utr == null || comment == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CLOSURE_DETAILS_REQUIRED", "UTR and comment are required for an offline closure");
        }
        PayoutBatch batch = batchRepository.findByReferenceForUpdate(reference)
                .orElseThrow(() -> ApiException.notFound("Settlement batch " + reference + " not found"));
        if (!CLOSABLE.contains(batch.getStatus())) {
            throw ApiException.conflict("BATCH_ALREADY_CLOSED", "Settlement batch " + reference + " is already " + label(batch.getStatus()));
        }
        PayoutBatchStatus from = batch.getStatus();
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);
        int paid = 0;
        for (Settlement s : settlementRepository.findByBatch(batch)) {
            if (!UNPAID.contains(s.getStatus())) continue;
            if (s.getPayoutReference() == null) {
                s.setPayoutReference(nextPayoutReference(today));
            }
            s.setStatus(SettlementStatus.PAID);
            s.setHoldReason(null);
            s.setPayoutDate(today);
            s.setBankReference(utr.length() > 40 ? utr.substring(0, 40) : utr);
            s.setUpdatedAt(now);
            settlementRepository.save(s);
            notifyWorkshop(s, today);
            paid++;
        }
        if (paid == 0) {
            throw ApiException.conflict("NOTHING_TO_CLOSE", "Settlement batch " + reference + " has no unpaid bookings");
        }
        batch.setStatus(PayoutBatchStatus.PAID);
        batch.setUtr(utr);
        batch.setPaymentMode(trim(request.paymentMode()) != null ? request.paymentMode().trim() : "BANK_TRANSFER");
        batch.setNote(comment);
        batch.setFailureReason(null);
        batch.setPaidAt(now);
        batch.setUpdatedAt(now);
        batchRepository.save(batch);
        auditLogService.record(AUDIT_ENTITY, batch.getId(), adminId, from.name(), PayoutBatchStatus.PAID.name(),
                "OFFLINE_CLOSURE", "UTR " + utr + " · " + paid + " booking(s) · " + comment);
        return batch(reference);
    }

    /** WPO-YYYYMMDD-NNNNNN from the same per-day sequence the platform's payout run uses (row lock). */
    private String nextPayoutReference(LocalDate day) {
        batchRepository.insertSequenceIfMissing(day);
        int next = batchRepository.lockSequence(day) + 1;
        batchRepository.updateSequence(day, next);
        return "WPO-" + day.format(REF_DATE) + "-" + String.format("%06d", next);
    }

    private void notifyWorkshop(Settlement s, LocalDate day) {
        notificationRepository.save(Notification.builder()
                .user(s.getMechanic())
                .title("Payout paid")
                .message("Payout paid: INR " + scale(s.getFinalPayout()) + " for booking " + s.getBooking().getBookingNumber()
                        + " (" + s.getPayoutReference() + ") on " + day)
                .notificationType(NotificationType.SETTLEMENT_PAID)
                .isRead(false)
                .relatedEntityType("Settlement")
                .relatedEntityId(s.getId())
                .build());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Gross advance = sum of the batch's advances; platform charges = handling fees + TCS; net = what's paid. */
    private SettlementBatchRow row(PayoutBatch b) {
        List<Settlement> members = settlementRepository.findByBatch(b);
        BigDecimal gross = sum(members.stream().map(s -> scale(s.getAdvanceCollected())));
        BigDecimal charges = sum(members.stream().map(s -> scale(s.getAdvanceHandlingFee()).add(scale(s.getTcsDeduction()))));
        User w = b.getMechanic();
        return new SettlementBatchRow(b.getBatchReference(), w.getId(), code(w.getId()), w.getWorkshopName(), w.getArea(),
                b.getBatchDate(), members.size(), gross, charges, ZERO, scale(b.getTotalAmount()), label(b.getStatus()),
                b.getUtr(), b.getPaidAt());
    }

    static String label(PayoutBatchStatus s) {
        return switch (s) {
            case TO_BE_PAID -> "PENDING";
            case PROCESSING -> "PROCESSING";
            case PAID -> "COMPLETED";
            case FAILED -> "FAILED";
            case PARTIALLY_PAID -> "EXCEPTION";
        };
    }

    private Booking findBooking(String ref) {
        if (blank(ref)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BOOKING_ID_REQUIRED", "Enter a Booking ID");
        }
        String r = ref.trim();
        List<Booking> byNumber = bookingRepository.findByBookingNumberWithParties(r);
        if (!byNumber.isEmpty()) {
            return byNumber.get(0);
        }
        if (r.chars().allMatch(Character::isDigit)) {
            return bookingRepository.findByIdWithParties(Long.parseLong(r))
                    .orElseThrow(() -> ApiException.notFound("No booking found for " + r));
        }
        throw ApiException.notFound("No booking found for " + r);
    }

    private String vehicleNumber(Booking b) {
        if (b.getVehicleUser() == null || b.getVehicle() == null) return null;
        return userVehicleRepository.findFirstByUserIdAndVehicleId(b.getVehicleUser().getId(), b.getVehicle().getId())
                .map(UserVehicle::getRegistrationNumber).orElse(null);
    }

    private BigDecimal advanceOf(Payment p) {
        return scale(p.getAdvanceAmount() != null ? p.getAdvanceAmount() : p.getAmount());
    }

    static String code(Long id) {
        return id == null ? null : String.format("WS-%03d", id);
    }

    private static String name(User u) {
        if (u == null) return null;
        String n = Stream.of(u.getFirstName(), u.getLastName()).filter(x -> x != null && !x.isBlank())
                .collect(Collectors.joining(" "));
        return n.isBlank() ? u.getEmail() : n;
    }

    private static int count(Map<String, Long> byStatus, String status) {
        return byStatus.getOrDefault(status, 0L).intValue();
    }

    private static BigDecimal sum(Stream<BigDecimal> values) {
        return values.filter(Objects::nonNull).reduce(ZERO, BigDecimal::add);
    }

    private static BigDecimal scale(BigDecimal v) {
        return v == null ? ZERO : v.setScale(2, RoundingMode.HALF_UP);
    }

    private static String join(String... parts) {
        return Stream.of(parts).filter(x -> x != null && !x.isBlank()).collect(Collectors.joining(" · "));
    }

    private static boolean contains(String needle, String... values) {
        String n = needle.trim().toLowerCase(Locale.ROOT);
        return Stream.of(values).filter(Objects::nonNull).anyMatch(v -> v.toLowerCase(Locale.ROOT).contains(n));
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String trim(String s) {
        return blank(s) ? null : s.trim();
    }
}
