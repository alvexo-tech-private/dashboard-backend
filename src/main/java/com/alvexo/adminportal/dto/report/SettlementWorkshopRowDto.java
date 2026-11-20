package com.alvexo.adminportal.dto.report;

import java.math.BigDecimal;

/** One workshop's settlements in the range, summed (ADMIN-US-20 BR-04: workshop-wise view). */
public record SettlementWorkshopRowDto(
        Long workshopId,
        String workshopName,
        long bookings,
        BigDecimal advanceCollected,
        BigDecimal advanceHandlingFee,
        BigDecimal netPay,
        BigDecimal tcsDeduction,
        BigDecimal finalPayout,
        BigDecimal paidAmount,
        BigDecimal outstandingAmount,
        long failedOrOnHold) {
}
