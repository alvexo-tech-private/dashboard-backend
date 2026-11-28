package com.alvexo.adminportal.entity;

/**
 * Day-wise workshop payout batch (written by vehicle-service-booking-api). Portal labels (ADMIN-US-11 BR-18):
 * TO_BE_PAID = Pending, PROCESSING = Processing, PAID = Completed, FAILED = Failed, PARTIALLY_PAID = Exception.
 */
public enum PayoutBatchStatus {
    TO_BE_PAID,
    PROCESSING,
    PAID,
    PARTIALLY_PAID,
    FAILED
}
