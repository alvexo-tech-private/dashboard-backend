package com.alvexo.adminportal.controller;

import com.alvexo.adminportal.config.AdminPrincipal;
import com.alvexo.adminportal.dto.MyApiResponse;
import com.alvexo.adminportal.dto.PagedResponse;
import com.alvexo.adminportal.dto.report.BookingReportDto;
import com.alvexo.adminportal.dto.report.RegistrationReportDto;
import com.alvexo.adminportal.dto.report.SettlementLineDto;
import com.alvexo.adminportal.dto.report.SettlementReportDto;
import com.alvexo.adminportal.dto.report.SettlementWorkshopRowDto;
import com.alvexo.adminportal.dto.report.WorkshopPerformanceRowDto;
import com.alvexo.adminportal.entity.BookingStatus;
import com.alvexo.adminportal.entity.SettlementStatus;
import com.alvexo.adminportal.report.DateRange;
import com.alvexo.adminportal.report.ExportFormat;
import com.alvexo.adminportal.report.ReportExportService;
import com.alvexo.adminportal.report.ReportType;
import com.alvexo.adminportal.service.OperationsReportService;
import com.alvexo.adminportal.service.SettlementReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Read-only reports. Each report has a JSON endpoint for the screen and an
 * {@code /export} endpoint that returns the same rows as CSV or XLSX and
 * writes the export to the audit log.
 *
 * Operational reports (bookings, registrations, workshop performance) are
 * open to every admin role. Settlement reports are Finance Admin and Super
 * Admin only (ADMIN-US-20 BR-19, US-22 BR-18).
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','FINANCE_ADMIN','OPERATIONS_ADMIN')")
public class ReportController {

    private static final String FINANCE = "hasAnyRole('SUPER_ADMIN','FINANCE_ADMIN')";

    private final OperationsReportService operationsReportService;
    private final SettlementReportService settlementReportService;
    private final ReportExportService reportExportService;

    // ---------------------------------------------------------------- bookings

    @GetMapping("/bookings")
    public MyApiResponse<BookingReportDto> bookings(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Long workshopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return MyApiResponse.success(operationsReportService.bookings(DateRange.of(from, to), status, workshopId, page, size));
    }

    @GetMapping("/bookings/export")
    public ResponseEntity<byte[]> exportBookings(
            @RequestParam String format,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Long workshopId,
            @AuthenticationPrincipal AdminPrincipal principal) {
        DateRange range = DateRange.of(from, to);
        return reportExportService.export(ReportType.BOOKINGS, ExportFormat.parse(format), range,
                filters("status", status, "workshopId", workshopId), OperationsReportService.BOOKING_COLUMNS,
                operationsReportService.allBookings(range, status, workshopId), principal.id());
    }

    // ----------------------------------------------------------- registrations

    @GetMapping("/registrations")
    public MyApiResponse<RegistrationReportDto> registrations(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return MyApiResponse.success(operationsReportService.registrations(DateRange.of(from, to)));
    }

    @GetMapping("/registrations/export")
    public ResponseEntity<byte[]> exportRegistrations(
            @RequestParam String format,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal AdminPrincipal principal) {
        DateRange range = DateRange.of(from, to);
        return reportExportService.export(ReportType.REGISTRATIONS, ExportFormat.parse(format), range,
                Map.of(), OperationsReportService.REGISTRATION_COLUMNS,
                operationsReportService.registrations(range).days(), principal.id());
    }

    // ---------------------------------------------------- workshop performance

    @GetMapping("/workshop-performance")
    public MyApiResponse<PagedResponse<WorkshopPerformanceRowDto>> workshopPerformance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return MyApiResponse.success(operationsReportService.workshopPerformance(DateRange.of(from, to), page, size));
    }

    @GetMapping("/workshop-performance/export")
    public ResponseEntity<byte[]> exportWorkshopPerformance(
            @RequestParam String format,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal AdminPrincipal principal) {
        DateRange range = DateRange.of(from, to);
        return reportExportService.export(ReportType.WORKSHOP_PERFORMANCE, ExportFormat.parse(format), range,
                Map.of(), OperationsReportService.WORKSHOP_PERFORMANCE_COLUMNS,
                operationsReportService.allWorkshopPerformance(range), principal.id());
    }

    // ------------------------------------------------------------- settlements

    @GetMapping("/settlements/by-workshop")
    @PreAuthorize(FINANCE)
    public MyApiResponse<SettlementReportDto<SettlementWorkshopRowDto>> settlementsByWorkshop(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) SettlementStatus status,
            @RequestParam(required = false) Long workshopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return MyApiResponse.success(
                settlementReportService.byWorkshop(DateRange.of(from, to), status, workshopId, page, size));
    }

    @GetMapping("/settlements/by-workshop/export")
    @PreAuthorize(FINANCE)
    public ResponseEntity<byte[]> exportSettlementsByWorkshop(
            @RequestParam String format,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) SettlementStatus status,
            @RequestParam(required = false) Long workshopId,
            @AuthenticationPrincipal AdminPrincipal principal) {
        DateRange range = DateRange.of(from, to);
        return reportExportService.export(ReportType.SETTLEMENTS_BY_WORKSHOP, ExportFormat.parse(format), range,
                filters("status", status, "workshopId", workshopId), SettlementReportService.WORKSHOP_COLUMNS,
                settlementReportService.allByWorkshop(range, status, workshopId), principal.id());
    }

    @GetMapping("/settlements/lines")
    @PreAuthorize(FINANCE)
    public MyApiResponse<SettlementReportDto<SettlementLineDto>> settlementLines(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) SettlementStatus status,
            @RequestParam(required = false) Long workshopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return MyApiResponse.success(
                settlementReportService.lines(DateRange.of(from, to), status, workshopId, page, size));
    }

    @GetMapping("/settlements/lines/export")
    @PreAuthorize(FINANCE)
    public ResponseEntity<byte[]> exportSettlementLines(
            @RequestParam String format,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) SettlementStatus status,
            @RequestParam(required = false) Long workshopId,
            @AuthenticationPrincipal AdminPrincipal principal) {
        DateRange range = DateRange.of(from, to);
        return reportExportService.export(ReportType.SETTLEMENT_LINES, ExportFormat.parse(format), range,
                filters("status", status, "workshopId", workshopId), SettlementReportService.LINE_COLUMNS,
                settlementReportService.allLines(range, status, workshopId), principal.id());
    }

    private static Map<String, Object> filters(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put(k1, v1);
        filters.put(k2, v2);
        return filters;
    }
}
