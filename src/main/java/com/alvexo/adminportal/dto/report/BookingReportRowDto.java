package com.alvexo.adminportal.dto.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One booking in the bookings report. */
public record BookingReportRowDto(
        Long id,
        String bookingNumber,
        LocalDateTime scheduledDateTime,
        String status,
        String serviceType,
        String bookingType,
        String channel,
        Long riderId,
        String riderName,
        Long workshopId,
        String workshopName,
        BigDecimal estimatedCost,
        BigDecimal actualCost,
        BigDecimal advancePaid,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt,
        String cancellationReason) {
}
