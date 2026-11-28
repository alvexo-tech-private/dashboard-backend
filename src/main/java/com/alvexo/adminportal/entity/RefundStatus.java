package com.alvexo.adminportal.entity;

/** Rider refund status (vehicle-service-booking-api sends refunds to Cashfree automatically). */
public enum RefundStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}
