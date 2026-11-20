package com.alvexo.adminportal.service;

import com.alvexo.adminportal.dto.PagedResponse;
import com.alvexo.adminportal.dto.report.SettlementLineDto;
import com.alvexo.adminportal.dto.report.SettlementReportDto;
import com.alvexo.adminportal.dto.report.SettlementStatusTotalDto;
import com.alvexo.adminportal.dto.report.SettlementTotalsDto;
import com.alvexo.adminportal.dto.report.SettlementWorkshopRowDto;
import com.alvexo.adminportal.entity.Settlement;
import com.alvexo.adminportal.entity.SettlementStatus;
import com.alvexo.adminportal.entity.User;
import com.alvexo.adminportal.report.DateRange;
import com.alvexo.adminportal.report.Names;
import com.alvexo.adminportal.report.ReportColumn;
import com.alvexo.adminportal.report.ReportExportService;
import com.alvexo.adminportal.repository.ReportQueryRepository;
import com.alvexo.adminportal.repository.SettlementRepository;
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
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Settlement report (ADMIN-US-20) over the booking API's per-booking
 * {@code settlements} table, filtered by settlement date. Two views: a
 * workshop-wise summary (BR-04) and the booking lines behind it (BR-10
 * drill-down). Amounts are summed from the stored values, never recalculated
 * (BR-15), and bank account details are never read (BR-21).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementReportService {

    private static final Sort LINE_ORDER = Sort.by(Sort.Order.desc("settlementDate"), Sort.Order.desc("id"));

    private final SettlementRepository settlementRepository;
    private final UserRepository userRepository;
    private final ReportQueryRepository reportQueryRepository;

    public static final List<ReportColumn<SettlementWorkshopRowDto>> WORKSHOP_COLUMNS = List.of(
            ReportColumn.of("Workshop ID", SettlementWorkshopRowDto::workshopId),
            ReportColumn.of("Workshop", SettlementWorkshopRowDto::workshopName),
            ReportColumn.of("Bookings", SettlementWorkshopRowDto::bookings),
            ReportColumn.of("Advance Collected (INR)", SettlementWorkshopRowDto::advanceCollected),
            ReportColumn.of("Handling Fee (INR)", SettlementWorkshopRowDto::advanceHandlingFee),
            ReportColumn.of("Net Pay (INR)", SettlementWorkshopRowDto::netPay),
            ReportColumn.of("TCS (INR)", SettlementWorkshopRowDto::tcsDeduction),
            ReportColumn.of("Final Payout (INR)", SettlementWorkshopRowDto::finalPayout),
            ReportColumn.of("Paid (INR)", SettlementWorkshopRowDto::paidAmount),
            ReportColumn.of("Outstanding (INR)", SettlementWorkshopRowDto::outstandingAmount),
            ReportColumn.of("Failed or On Hold", SettlementWorkshopRowDto::failedOrOnHold));

    public static final List<ReportColumn<SettlementLineDto>> LINE_COLUMNS = List.of(
            ReportColumn.of("Settlement ID", SettlementLineDto::settlementId),
            ReportColumn.of("Settlement Date", SettlementLineDto::settlementDate),
            ReportColumn.of("Workshop ID", SettlementLineDto::workshopId),
            ReportColumn.of("Workshop", SettlementLineDto::workshopName),
            ReportColumn.of("Booking Number", SettlementLineDto::bookingNumber),
            ReportColumn.of("Advance Collected (INR)", SettlementLineDto::advanceCollected),
            ReportColumn.of("Handling Fee (INR)", SettlementLineDto::advanceHandlingFee),
            ReportColumn.of("Net Pay (INR)", SettlementLineDto::netPay),
            ReportColumn.of("TCS (INR)", SettlementLineDto::tcsDeduction),
            ReportColumn.of("Final Payout (INR)", SettlementLineDto::finalPayout),
            ReportColumn.of("Status", SettlementLineDto::status),
            ReportColumn.of("Expected Settlement Date", SettlementLineDto::expectedSettlementDate),
            ReportColumn.of("Payout Date", SettlementLineDto::payoutDate),
            ReportColumn.of("Payout Reference", SettlementLineDto::payoutReference),
            ReportColumn.of("Bank Reference", SettlementLineDto::bankReference),
            ReportColumn.of("Gateway Payout ID", SettlementLineDto::gatewayPayoutId),
            ReportColumn.of("Hold Reason", SettlementLineDto::holdReason));

    // ------------------------------------------------------- workshop-wise view

    public SettlementReportDto<SettlementWorkshopRowDto> byWorkshop(DateRange range, SettlementStatus status,
                                                                    Long workshopId, int page, int size) {
        Specification<Settlement> filter = settlementFilter(range, status, workshopId);
        int pageSize = OperationsReportService.clamp(size);
        int pageIndex = Math.max(page, 0);

        long workshops = reportQueryRepository.countSettlementWorkshops(filter);
        List<SettlementWorkshopRowDto> rows = workshopRows(filter, pageIndex * pageSize, pageSize);
        int totalPages = (int) ((workshops + pageSize - 1) / pageSize);
        PagedResponse<SettlementWorkshopRowDto> paged = PagedResponse.<SettlementWorkshopRowDto>builder()
                .content(rows).page(pageIndex).size(pageSize).totalElements(workshops).totalPages(totalPages)
                .last(pageIndex + 1 >= totalPages).build();
        return withTotals(filter, workshops, paged);
    }

    public List<SettlementWorkshopRowDto> allByWorkshop(DateRange range, SettlementStatus status, Long workshopId) {
        return workshopRows(settlementFilter(range, status, workshopId), 0, ReportExportService.fetchLimit());
    }

    private List<SettlementWorkshopRowDto> workshopRows(Specification<Settlement> filter, int offset, int limit) {
        List<Object[]> rows = reportQueryRepository.settlementsByWorkshop(filter, offset, limit);
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, User> workshops = userRepository.findAllById(rows.stream().map(r -> (Long) r[0]).toList()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return rows.stream().map(r -> new SettlementWorkshopRowDto(
                (Long) r[0], Names.workshop(workshops.get((Long) r[0])), (Long) r[1],
                money(r[2]), money(r[3]), money(r[4]), money(r[5]), money(r[6]), money(r[7]), money(r[8]),
                (Long) r[9])).toList();
    }

    // ---------------------------------------------------------- booking lines

    public SettlementReportDto<SettlementLineDto> lines(DateRange range, SettlementStatus status, Long workshopId,
                                                        int page, int size) {
        Specification<Settlement> filter = settlementFilter(range, status, workshopId);
        Page<Settlement> rows = settlementRepository.findAll(filter,
                PageRequest.of(Math.max(page, 0), OperationsReportService.clamp(size), LINE_ORDER));
        return withTotals(filter, reportQueryRepository.countSettlementWorkshops(filter),
                PagedResponse.from(rows, SettlementReportService::toLine));
    }

    public List<SettlementLineDto> allLines(DateRange range, SettlementStatus status, Long workshopId) {
        return settlementRepository.findAll(settlementFilter(range, status, workshopId),
                        PageRequest.of(0, ReportExportService.fetchLimit(), LINE_ORDER))
                .map(SettlementReportService::toLine).getContent();
    }

    static SettlementLineDto toLine(Settlement s) {
        return new SettlementLineDto(
                s.getId(), s.getSettlementDate(),
                s.getMechanic() == null ? null : s.getMechanic().getId(), Names.workshop(s.getMechanic()),
                s.getBooking() == null ? null : s.getBooking().getId(),
                s.getBooking() == null ? null : s.getBooking().getBookingNumber(),
                s.getAdvanceCollected(), s.getAdvanceHandlingFee(), s.getNetPay(), s.getTcsDeduction(),
                s.getFinalPayout(), s.getStatus() == null ? null : s.getStatus().name(),
                s.getExpectedSettlementDate(), s.getPayoutDate(), s.getPayoutReference(), s.getBankReference(),
                s.getProviderPayoutId(), s.getHoldReason());
    }

    // ----------------------------------------------------------------- totals

    private <T> SettlementReportDto<T> withTotals(Specification<Settlement> filter, long workshops, PagedResponse<T> rows) {
        Map<SettlementStatus, Object[]> byStatus = new EnumMap<>(SettlementStatus.class);
        for (Object[] row : reportQueryRepository.settlementStatusTotals(filter)) {
            byStatus.put((SettlementStatus) row[0], row);
        }
        long count = 0;
        BigDecimal advance = BigDecimal.ZERO, fee = BigDecimal.ZERO, net = BigDecimal.ZERO,
                tcs = BigDecimal.ZERO, payout = BigDecimal.ZERO;
        List<SettlementStatusTotalDto> statusTotals = new ArrayList<>();
        for (SettlementStatus status : SettlementStatus.values()) {
            Object[] row = byStatus.get(status);
            long statusCount = row == null ? 0 : (Long) row[1];
            BigDecimal statusPayout = row == null ? BigDecimal.ZERO : money(row[6]);
            statusTotals.add(new SettlementStatusTotalDto(status.name(), statusCount, statusPayout));
            if (row != null) {
                count += statusCount;
                advance = advance.add(money(row[2]));
                fee = fee.add(money(row[3]));
                net = net.add(money(row[4]));
                tcs = tcs.add(money(row[5]));
                payout = payout.add(statusPayout);
            }
        }
        return new SettlementReportDto<>(new SettlementTotalsDto(count, workshops, advance, fee, net, tcs, payout),
                statusTotals, rows);
    }

    static Specification<Settlement> settlementFilter(DateRange range, SettlementStatus status, Long workshopId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.between(root.get("settlementDate"), range.from(), range.to()));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (workshopId != null) {
                predicates.add(cb.equal(root.get("mechanic").get("id"), workshopId));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static BigDecimal money(Object value) {
        return value == null ? BigDecimal.ZERO : (BigDecimal) value;
    }
}
