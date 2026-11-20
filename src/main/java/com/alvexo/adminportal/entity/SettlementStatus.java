package com.alvexo.adminportal.entity;

/**
 * Workshop payout status of one booking's settlement — mirrors
 * com.alvexo.bookingapp.model.SettlementStatus:
 * AWAITING_PAYOUT -> PROCESSING -> PAID | FAILED; FAILED -> PROCESSING (retry);
 * any open state -> ON_HOLD -> AWAITING_PAYOUT. NO_SETTLEMENT_REQUIRED is final.
 */
public enum SettlementStatus {
    AWAITING_PAYOUT,
    PROCESSING,
    PAID,
    FAILED,
    ON_HOLD,
    NO_SETTLEMENT_REQUIRED
}
