package com.alvexo.adminportal.dto.report;

import java.math.BigDecimal;

/**
 * One workshop's bookings in the range. {@code completionRate} is completed /
 * total as a percentage; {@code averageRating} covers reviews written in the range.
 */
public record WorkshopPerformanceRowDto(
        Long workshopId,
        String workshopName,
        String city,
        long totalBookings,
        long completed,
        long cancelled,
        long rejected,
        BigDecimal completionRate,
        BigDecimal completedBookingValue,
        BigDecimal averageRating,
        long reviews) {
}
