package com.alvexo.adminportal.dto.report;

import com.alvexo.adminportal.dto.PagedResponse;

import java.util.List;

/** Settlement report: range totals, totals by status, and one page of workshop or booking rows. */
public record SettlementReportDto<T>(SettlementTotalsDto totals, List<SettlementStatusTotalDto> byStatus,
                                     PagedResponse<T> rows) {
}
