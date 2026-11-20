package com.alvexo.adminportal.service;

import com.alvexo.adminportal.dto.report.SettlementReportDto;
import com.alvexo.adminportal.dto.report.SettlementWorkshopRowDto;
import com.alvexo.adminportal.entity.SettlementStatus;
import com.alvexo.adminportal.entity.User;
import com.alvexo.adminportal.report.DateRange;
import com.alvexo.adminportal.repository.ReportQueryRepository;
import com.alvexo.adminportal.repository.SettlementRepository;
import com.alvexo.adminportal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Pure unit tests: every repository is mocked, nothing touches a database. */
class SettlementReportServiceTest {

    private ReportQueryRepository reportQueryRepository;
    private UserRepository userRepository;
    private SettlementReportService service;

    private final DateRange range = DateRange.of(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

    @BeforeEach
    void setUp() {
        reportQueryRepository = mock(ReportQueryRepository.class);
        userRepository = mock(UserRepository.class);
        service = new SettlementReportService(mock(SettlementRepository.class), userRepository, reportQueryRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void byWorkshopSumsStoredAmountsAndPagesTheWorkshops() {
        when(reportQueryRepository.countSettlementWorkshops(any(Specification.class))).thenReturn(21L);
        when(reportQueryRepository.settlementsByWorkshop(any(Specification.class), eq(20), eq(20))).thenReturn(List.<Object[]>of(
                new Object[]{5L, 2L, new BigDecimal("200.00"), new BigDecimal("10.00"), new BigDecimal("190.00"),
                        new BigDecimal("1.90"), new BigDecimal("188.10"), new BigDecimal("94.05"),
                        new BigDecimal("94.05"), 1L}));
        when(userRepository.findAllById(anyCollection()))
                .thenReturn(List.of(User.builder().id(5L).workshopName("Rao Motors").build()));
        when(reportQueryRepository.settlementStatusTotals(any(Specification.class))).thenReturn(List.<Object[]>of(
                new Object[]{SettlementStatus.PAID, 3L, new BigDecimal("300.00"), new BigDecimal("15.00"),
                        new BigDecimal("285.00"), new BigDecimal("2.85"), new BigDecimal("282.15")},
                new Object[]{SettlementStatus.FAILED, 1L, new BigDecimal("100.00"), new BigDecimal("5.00"),
                        new BigDecimal("95.00"), new BigDecimal("0.95"), new BigDecimal("94.05")}));

        SettlementReportDto<SettlementWorkshopRowDto> report = service.byWorkshop(range, null, null, 1, 20);

        assertEquals(4, report.totals().settlements());
        assertEquals(21, report.totals().workshops());
        assertEquals(new BigDecimal("376.20"), report.totals().finalPayout());
        assertEquals(new BigDecimal("400.00"), report.totals().advanceCollected());
        assertEquals(SettlementStatus.values().length, report.byStatus().size());

        assertEquals(2, report.rows().getTotalPages());
        assertTrue(report.rows().isLast());
        SettlementWorkshopRowDto row = report.rows().getContent().get(0);
        assertEquals("Rao Motors", row.workshopName());
        assertEquals(new BigDecimal("94.05"), row.outstandingAmount());
        assertEquals(1, row.failedOrOnHold());
    }

    @Test
    @SuppressWarnings("unchecked")
    void emptyRangeGivesZeroTotalsNotErrors() {
        when(reportQueryRepository.settlementsByWorkshop(any(Specification.class), anyInt(), anyInt())).thenReturn(List.of());
        when(reportQueryRepository.settlementStatusTotals(any(Specification.class))).thenReturn(List.of());

        SettlementReportDto<SettlementWorkshopRowDto> report = service.byWorkshop(range, null, null, 0, 20);

        assertEquals(0, report.totals().settlements());
        assertEquals(BigDecimal.ZERO, report.totals().finalPayout());
        assertTrue(report.rows().getContent().isEmpty());
        assertTrue(report.rows().isLast());
    }
}
