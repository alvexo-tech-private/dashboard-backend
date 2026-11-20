package com.alvexo.adminportal.dto.report;

import com.alvexo.adminportal.dto.PagedResponse;

import java.util.List;

/** Bookings report: totals for the whole filtered range plus one page of rows. */
public record BookingReportDto(long total, List<StatusCountDto> statusCounts, PagedResponse<BookingReportRowDto> rows) {
}
