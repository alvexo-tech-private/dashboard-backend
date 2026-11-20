package com.alvexo.adminportal.dto.report;

import java.math.BigDecimal;

/** Range totals over every settlement matching the filters (ADMIN-US-20 BR-08). */
public record SettlementTotalsDto(
        long settlements,
        long workshops,
        BigDecimal advanceCollected,
        BigDecimal advanceHandlingFee,
        BigDecimal netPay,
        BigDecimal tcsDeduction,
        BigDecimal finalPayout) {
}
