package com.alvexo.adminportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Finance module (ADMIN-EPIC-06): Finance Dashboard (US-11), Booking Financial Breakdown (US-12) and Workshop
 * Settlement Summary (US-13). Values come from the platform backend's records and are read-only here.
 *
 * <p>Settlement status labels (US-11 BR-18): PENDING, PROCESSING, COMPLETED, FAILED, EXCEPTION.
 */
public final class FinanceDtos {

    private FinanceDtos() {
    }

    /** Payments collected through Cashfree on the selected date (US-11 BR-04/05). */
    public record CollectionSummary(BigDecimal advanceTotal, BigDecimal platformFeeTotal, BigDecimal grossTotal,
                                    int transactionCount) {
    }

    /** Gateway refunds completed on the date — shown for transparency only (BR-27). */
    public record RefundSummary(BigDecimal amount, int count) {
    }

    /** Settlement batch counts for the date (AC-06/07). */
    public record BatchCounts(int total, int pending, int processing, int completed, int failed, int exception) {
    }

    /** One workshop's consolidated settlement batch (US-11 BR-20, US-13 BR-05). */
    public record SettlementBatchRow(String batchReference, Long workshopId, String workshopCode, String workshopName,
                                     String area, LocalDate settlementDate, int bookingCount, BigDecimal grossAdvance,
                                     BigDecimal platformCharges, BigDecimal adjustments, BigDecimal netSettlement,
                                     String status, String gatewayReference, LocalDateTime completedAt) {
    }

    public record FinanceDashboard(LocalDate date, CollectionSummary collection, RefundSummary refunds,
                                   BatchCounts batches, List<SettlementBatchRow> workshopSettlements) {
    }

    /** A booking inside a batch (US-13 BR-10). */
    public record BatchBooking(Long bookingId, String bookingNumber, String riderName, String vehicleNumber,
                               LocalDate serviceDate, BigDecimal advance, BigDecimal charges,
                               BigDecimal netContribution, String settlementStatus, String paymentStatus,
                               String orderId, String cashfreePaymentId, String payoutReference) {
    }

    public record HistoryEntry(LocalDateTime at, String action, String detail, String by) {
    }

    public record SettlementBatchDetail(SettlementBatchRow batch, String note, String failureReason,
                                        boolean exception, List<BatchBooking> bookings, List<HistoryEntry> history) {
    }

    public record Party(Long id, String code, String name, String phone, String area) {
    }

    public record PaymentDetail(String orderId, String cashfreePaymentId, LocalDateTime paymentDate, String method,
                                BigDecimal advance, BigDecimal platformFee, BigDecimal gross, String status) {
    }

    /** Platform charges, shown separately from the rider's advance (US-12 BR-06). */
    public record Charges(BigDecimal handlingFee, BigDecimal tcs, BigDecimal workshopShare, BigDecimal bykCommission,
                          BigDecimal gatewayCharges) {
    }

    public record RefundDetail(String reason, String status, BigDecimal amount, LocalDateTime initiatedAt,
                               LocalDateTime completedAt, String cashfreeRefundId, String lastError) {
    }

    public record BookingSettlement(String status, String batchReference, String batchStatus, LocalDate settlementDate,
                                    BigDecimal amount, String payoutReference, String gatewayReference,
                                    LocalDate expectedDate, String holdReason) {
    }

    /** US-12: one booking's money — payment, charges, refund, settlement — and any exception. */
    public record BookingFinance(Long bookingId, String bookingNumber, String bookingStatus, LocalDate serviceDate,
                                 Party rider, Party workshop, PaymentDetail payment, Charges charges,
                                 List<RefundDetail> refunds, BookingSettlement settlement, String exception) {
    }

    /**
     * Super Admin offline closure of a batch paid outside the gateway (US-14 BR-14 / US-18): UTR and a comment are
     * mandatory and the action is audited.
     */
    public record OfflineClosureRequest(@NotBlank @Size(max = 100) String utr,
                                        @NotBlank @Size(max = 500) String comment,
                                        @Size(max = 30) String paymentMode) {
    }

    // ── Finance Dashboard (BYK Finance Portal mock-up): period cards + recent transactions ──

    /** A summary card: amount and count for the period, previous period, change %, and a daily trend for the sparkline. */
    public record OverviewCard(BigDecimal amount, int count, BigDecimal previousAmount, BigDecimal changePercent,
                               List<BigDecimal> trend) {
    }

    /**
     * One rider payment, by BYK Booking ID. status: PAID (workshop paid), TO_BE_PAID, REFUNDED, REFUND_PROCESSING,
     * CANCELLED (cancelled, advance not refundable), NO_PAYMENT (₹0 booking — detailedStatus SAME_DAY, PICKUP,
     * WALK_IN or CANCELLED; date = when it was booked); detailedStatus adds IN_HOLDING, NOT_COMPLETED, PROCESSING …
     */
    public record TransactionRow(Long bookingId, String bookingNumber, String customerName, Long workshopId,
                                 String workshopName, LocalDateTime paymentDate, BigDecimal amount, String status,
                                 String detailedStatus, String orderId, String cashfreePaymentId,
                                 String cashfreeRefundId) {
    }

    public record TransactionPage(List<TransactionRow> rows, int page, int size, long total) {
    }

    public record FinanceOverview(LocalDate from, LocalDate to, OverviewCard customerPayments,
                                  OverviewCard workshopPayable, OverviewCard settled, OverviewCard refunds,
                                  List<TransactionRow> recentTransactions, LocalDateTime lastUpdated) {
    }
}
