package com.alvexo.adminportal.controller;

import com.alvexo.adminportal.config.AdminPrincipal;
import com.alvexo.adminportal.dto.FinanceDtos.BookingFinance;
import com.alvexo.adminportal.dto.FinanceDtos.FinanceDashboard;
import com.alvexo.adminportal.dto.FinanceDtos.FinanceOverview;
import com.alvexo.adminportal.dto.FinanceDtos.TransactionPage;
import com.alvexo.adminportal.dto.FinanceDtos.OfflineClosureRequest;
import com.alvexo.adminportal.dto.FinanceDtos.SettlementBatchDetail;
import com.alvexo.adminportal.dto.FinanceDtos.SettlementBatchRow;
import com.alvexo.adminportal.dto.MyApiResponse;
import com.alvexo.adminportal.service.FinanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Finance module (ADMIN-EPIC-06): read-only views of Cashfree payments, automatic refunds and the day-wise workshop
 * settlement batches. Finance Admin and Super Admin only (US-11 AC-22). The single write — offline closure of a
 * batch paid outside the gateway — is Super Admin only.
 */
@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','FINANCE_ADMIN')")
public class FinanceController {

    private final FinanceService financeService;

    /** Finance Dashboard: the four period cards (default last 30 days) and the 5 newest transactions. */
    @GetMapping("/overview")
    public MyApiResponse<FinanceOverview> overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return MyApiResponse.success(financeService.overview(from, to));
    }

    /** Rider payments (View All / filters): bookingId, customer, workshop, paymentDate, q, status; paging. */
    @GetMapping("/transactions")
    public MyApiResponse<TransactionPage> transactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String bookingId,
            @RequestParam(required = false) String customer,
            @RequestParam(required = false) String workshop,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paymentDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return MyApiResponse.success(financeService.transactions(from, to, q, status, bookingId, customer, workshop, paymentDate,
                page, size));
    }

    /** US-11: collections, refunds and settlement batches for a date (default today). */
    @GetMapping("/dashboard")
    public MyApiResponse<FinanceDashboard> dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return MyApiResponse.success(financeService.dashboard(date));
    }

    /** US-13: workshop-wise batches for a date; status = PENDING | PROCESSING | COMPLETED | FAILED | EXCEPTION. */
    @GetMapping("/settlements")
    public MyApiResponse<List<SettlementBatchRow>> settlements(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        return MyApiResponse.success(financeService.batches(date, status, search));
    }

    /** US-13 drill-down: a batch's bookings and history. */
    @GetMapping("/settlements/{batchReference}")
    public MyApiResponse<SettlementBatchDetail> settlement(@PathVariable String batchReference) {
        return MyApiResponse.success(financeService.batch(batchReference));
    }

    /** US-12: a booking's payment, charges, refund and settlement, by BYK Booking ID. */
    @GetMapping("/bookings/{bookingRef}")
    public MyApiResponse<BookingFinance> booking(@PathVariable String bookingRef) {
        return MyApiResponse.success(financeService.booking(bookingRef));
    }

    /** Offline closure of a batch paid outside the gateway — UTR + comment mandatory, audited. */
    @PostMapping("/settlements/{batchReference}/offline-closure")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public MyApiResponse<SettlementBatchDetail> closeOffline(@PathVariable String batchReference,
                                                             @Valid @RequestBody OfflineClosureRequest request,
                                                             @AuthenticationPrincipal AdminPrincipal principal) {
        return MyApiResponse.success(financeService.closeOffline(batchReference, request, principal.id()));
    }
}
