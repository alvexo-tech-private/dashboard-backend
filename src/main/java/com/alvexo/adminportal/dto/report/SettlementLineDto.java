package com.alvexo.adminportal.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One booking's settlement, as stored by the booking API — amounts are shown,
 * never recalculated (ADMIN-US-20 BR-15). Bank account details are not included.
 */
public record SettlementLineDto(
        Long settlementId,
        LocalDate settlementDate,
        Long workshopId,
        String workshopName,
        Long bookingId,
        String bookingNumber,
        BigDecimal advanceCollected,
        BigDecimal advanceHandlingFee,
        BigDecimal netPay,
        BigDecimal tcsDeduction,
        BigDecimal finalPayout,
        String status,
        LocalDate expectedSettlementDate,
        LocalDate payoutDate,
        String payoutReference,
        String bankReference,
        String gatewayPayoutId,
        String holdReason) {
}
