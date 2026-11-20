package com.alvexo.adminportal.dto.report;

import java.math.BigDecimal;

/** Settlement count and final-payout total for one status across the filtered range. */
public record SettlementStatusTotalDto(String status, long count, BigDecimal finalPayout) {
}
